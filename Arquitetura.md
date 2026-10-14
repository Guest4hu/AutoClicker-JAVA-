# Relatório de Arquitetura e Engenharia de Software: AutoClicker JavaFX

**Data:** Outubro de 2026  
**Autor:** Engenharia de Software / Arquitetura de Sistemas  
**Projeto:** AutoClicker-JAVA-  
**Escopo:** Diagnóstico de Testes, Análise Arquitetural, Catálogo de Problemas, Proposta de Reorganização Estrutural e Roteiro de Refatorações  

---

## 1. Diagnóstico do Ecossistema e Configuração Atual

### 1.1 Versões, Ferramentas e Configuração do Maven
O projeto é construído sobre a plataforma **Java 25** com interface gráfica em **JavaFX 21.0.6 (LTS)**, orquestrado pelo **Apache Maven** (com suporte ao wrapper `./mvnw` e `mvnw.cmd`).

* **Compilação (`pom.xml`):**
  * `<source>25</source>` e `<target>25</target>` configurados no `maven-compiler-plugin` (versão 3.13.0). Java 25 introduz recursos modernos de linguagem e melhorias na Foreign Function & Memory (FFM) API, mas trata-se de versão de desenvolvimento contínuo / bleeding edge em relação ao JavaFX 21.
  * O plugin `javafx-maven-plugin` (versão 0.0.8) está configurado com a classe principal modular `com.example.autoclicker/com.example.autoclicker.AutoClickerApplication` e a opção de JVM `--enable-native-access=javafx.graphics`, além de configurações para empacotamento modular com `jlink`.
* **Dependências de Produção:**
  * `org.openjfx:javafx-controls:21.0.6` e `javafx-fxml:21.0.6`: Interface visual e parsing de arquivos FXML.
  * `com.github.kwhat:jnativehook:2.2.2`: Captura global de eventos de teclado e mouse fora da janela da aplicação.
  * `com.fasterxml.jackson.core:jackson-databind:2.18.2` e `jackson-datatype-jsr310:2.18.2`: Serialização e desserialização de arquivos JSON para gravação/reprodução de macros.
* **Dependências de Teste:**
  * `org.junit.jupiter:junit-jupiter-api:5.12.1` e `junit-jupiter-engine:5.12.1` sob escopo `test`.
* **Problemas Identificados no `pom.xml`:**
  1. **Repositório JCenter Obsoleto:** O bloco `<repositories>` inclui `https://jcenter.bintray.com/`, serviço descontinuado e tornado somente-leitura em maio de 2021 pela JFrog. Pode causar timeouts em builds limpos.
  2. **Repositório GitHub Packages Redundante:** O repositório `https://maven.pkg.github.com/kwhat/jnativehook` exige token de autenticação nos arquivos `settings.xml` de ambientes padrão, mas a dependência `jnativehook 2.2.2` já se encontra disponível no Maven Central.
  3. **Configuração Frágil do Surefire:** O plugin `maven-surefire-plugin` (versão 3.2.5) define `<argLine>-Djava.awt.headless=false</argLine>`. Como analisado a seguir, essa flag quebra testes em qualquer ambiente de integração contínua (CI/CD) ou distribuições Linux headless.

---

### 1.2 Sistema de Módulos Java (`module-info.java`)
O módulo do projeto é definido em `src/main/java/module-info.java`:

```java
module com.example.autoclicker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.github.kwhat.jnativehook;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.example.autoclicker to javafx.fxml;
    opens com.example.autoclicker.controller to javafx.fxml;
    opens com.example.autoclicker.DTO to com.fasterxml.jackson.databind;
    opens com.example.autoclicker.domain to com.fasterxml.jackson.databind;
    opens com.example.autoclicker.enums to com.fasterxml.jackson.databind;

    exports com.example.autoclicker;
    exports com.example.autoclicker.domain;
    exports com.example.autoclicker.DTO;
    exports com.example.autoclicker.enums;
}
```

* **Coerência de `opens` e `exports`:**
  * O pacote `controller` é devidamente aberto (`opens ... to javafx.fxml`), permitindo que o `FXMLLoader` instancie e injete campos privados anotados com `@FXML`.
  * Os pacotes `DTO`, `domain` e `enums` são abertos ao `com.fasterxml.jackson.databind` para reflexão de serialização JSON.
* **Problemas de Módulo:**
  1. **Violação de Convenção de Nomes:** O pacote `com.example.autoclicker.DTO` utiliza acrônimo em caixa alta, violando as convenções canônicas de nomenclatura do Java (JLS §6.1). Qualquer renomeação para `com.example.autoclicker.dto` exige atualização coordenada de `module-info.java` e todos os imports do projeto.
  2. **Encapsulamento Incompleto vs Testes:** Os pacotes `service` e `config` não são exportados nem abertos. Como o Surefire atualmente executa os testes na classpath (unnamed module), isso não quebrou a compilação de testes, mas impedirá que os testes sejam modularizados no futuro sem parâmetros `--add-opens`.
  3. **Vazamento de Domínio no Jackson:** Abrir o pacote inteiro `domain` para o Jackson expõe modelos de negócio internos (`MacroActionSetting`), quando apenas DTOs de transporte deveriam ser alvos de serialização reflexiva.

---

### 1.3 Diagnóstico Profundo dos Testes Automatizados (Causa Raiz das Falhas)
Ao executar a suíte de testes com `./mvnw test`, a execução é interrompida com **7 testes executados, 1 Falha (Failure) e 1 Erro (Error)**:

```text
[ERROR] Failures: 
[ERROR]   FXMLValidationTest.testFXMLLoaderInstantiates:68 Erro ao carregar FXML e Controller no JavaFX Toolkit
[ERROR] Errors: 
[ERROR]   AutoClickerEngineTest.testLimitedClicksExecutionWithDriftCompensation:35 » NoClassDefFound Could not initialize class java.awt.GraphicsEnvironment$LocalGE
[INFO] Tests run: 7, Failures: 1, Errors: 1, Skipped: 0
```

#### A. Diagnóstico do Erro em `AutoClickerEngineTest`
* **Localização:** `src/test/java/com/example/autoclicker/AutoClickerEngineTest.java:35`
* **Causa Raiz Técnica:**
  1. O teste invoca `AutoClickerEngine.startAutoClicker()`. `AutoClickerEngine` herda de `AbstractEngine`.
  2. Em `AbstractEngine.java`, há um inicializador estático e chamadas diretas a `new java.awt.Robot()`.
  3. No `pom.xml`, o Surefire força `-Djava.awt.headless=false`.
  4. Em ambientes Linux corporativos, contêineres Docker ou distribuições com JRE headless (como `java-25-openjdk-headless` instalada no Fedora/RHEL deste ambiente), o binário nativo da biblioteca X11 do AWT (`/usr/lib/jvm/java-25-openjdk/lib/libawt_xawt.so`) **não existe**, pois apenas a `libawt_headless.so` está presente.
  5. A JVM tenta carregar `libawt_xawt.so`, dispara `UnsatisfiedLinkError`, corrompe a inicialização da classe `GraphicsEnvironment$LocalGE` e propaga `NoClassDefFoundError`.
* **Defeito Arquitetural Subjacente:** O teste automatizado não é um teste unitário: ele tenta controlar fisicamente o cursor do mouse e disparar cliques reais no sistema operacional. Como o motor de cliques está fortemente acoplado à classe concreta `java.awt.Robot` e não a uma interface abstrata (ex.: `InputSimulator`), é impossível testar a lógica de drift compensation, contagem de cliques, interrupção e intervalos de tempo sem hardware gráfico real conectado.

#### B. Diagnóstico da Falha em `FXMLValidationTest`
* **Localização:** `src/test/java/com/example/autoclicker/FXMLValidationTest.java:68`
* **Causa Raiz Técnica:**
  1. O teste inicializa o JavaFX Toolkit (`Platform.startup`) e tenta carregar `autoClicker-view.fxml` instanciando `AutoClickerController`.
  2. Durante o método `initialize()` do controller, é chamado `updateHotkeyDisplay()`.
  3. Esse método chama `hotkeyService.getHotkeyText()`.
  4. `HotkeyService` itera sobre as teclas mapeadas chamando `NativeKeyEvent.getKeyText(code)` (da biblioteca JNativeHook).
  5. Por debaixo dos panos, o método `NativeKeyEvent.getKeyText()` consulta o `java.awt.Toolkit.getDefaultToolkit()`, que por sua vez tenta inicializar o subsistema AWT X11 e falha pelo mesmo motivo (`UnsatisfiedLinkError` ao buscar `libawt_xawt.so`).
* **Defeito Arquitetural Subjacente:** Efeitos colaterais nativos e acoplamento estático dentro do método `initialize()` do controller. Não existe isolamento entre a camada de apresentação e os drivers nativos do sistema operacional.

---

### 1.4 Avaliação de Responsabilidades: Realidade da Base vs Premissa Inicial
> **Nota de Alinhamento Crítico:** O briefing inicial sugeria que *"o projeto é somente lógica e interface: não há persistência de dados nem comunicação com sistemas externos (banco, arquivos, HTTP)"*.
>
> A inspeção minuciosa da base de código revelou que **essa premissa não corresponde à realidade do projeto**:
> 1. **Persistência em Disco:** A classe `MacroStorageService` gerencia leitura e escrita persistente de arquivos `.json` na pasta local `storage/macros` através do Jackson ObjectMapper.
> 2. **Comunicação Nativa com o SO:** A classe `LinuxX11WindowService` utiliza a Foreign Function & Memory API (Panama FFM) para abrir handles nativos diretamente contra a biblioteca de sistema `libX11.so.6` (`XOpenDisplay`, `XGetWindowProperty`, `XFetchName`).
> 3. **Hooks de Hardware do SO:** O `HotkeyService` e `MacroService` instanciam hooks globais de kernel/X11 via `GlobalScreen` do JNativeHook.

Essa constatação é fundamental: o sistema não pode ser tratado como mero modelo de domínio em memória. Há fronteiras de I/O de disco e chamadas de sistema nativas que estão vazando diretamente para dentro da thread gráfica do JavaFX (`JavaFX Application Thread`), gerando riscos severos de travamento e condições de corrida.

---

## 2. Análise da Arquitetura Atual e Quebras de Padrões

O projeto pretendia adotar um modelo em camadas (Controllers, Domain, Services, DTOs, Enums). No entanto, a implementação atual apresenta quebras graves de padrões de projeto e acoplamento desordenado.

```
+-----------------------------------------------------------------------------------------+
|                                   ARQUITETURA ATUAL                                     |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|  [autoClicker-view.fxml] <-------------------+                                          |
|            |                                 | (FXML carrega e acopla                   |
|            v                                 |  ambas as abas num único controller)     |
|  +----------------------------------------------------+                                 |
|  | AutoClickerController ("God Controller" - 669 lin) |                                 |
|  +----------------------------------------------------+                                 |
|      |               |                 |          |                                     |
|      | (Instancia    | (Chama métodos  |          | (I/O bloqueante na thread de UI)    |
|      |  instâncias   |  estáticos)     |          v                                     |
|      |  novas)       |                 |    +---------------------+                     |
|      v               v                 |    | MacroStorageService |                     |
|  +-------------------------+           |    | (I/O síncrono JSON) |                     |
|  | AutoClickerEngine       |           |    +---------------------+                     |
|  | MacroEngine             |           |                                                |
|  | (Singleton Híbrido)     |           | (Invocação síncrona de C / X11 na thread UI)   |
|  +-------------------------+           v                                                |
|      |               |          +--------------------------+                            |
|      |               |          | LinuxX11WindowService    |                            |
|      v               v          | (Panama FFM libX11.so.6) |                            |
|  +---------------+ +----------+ +--------------------------+                            |
|  | java.awt.Robot| | Global-  |                                                         |
|  | (Acoplamento  | | Screen   |                                                         |
|  |  concreto)    | | (JNative)|                                                         |
|  +---------------+ +----------+                                                         |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### 2.1 O "God Controller" (`AutoClickerController`)
A premissa arquitetural de que as telas possuiriam *"controllers separados para cada tela"* foi completamente rompida:
* O arquivo `autoClicker-view.fxml` agrega duas telas inteiras em abas distintas (`AutoClicker` e `Macro`) sob um único controlador de 669 linhas.
* O `AutoClickerController` acumula dezenas de responsabilidades conflitantes:
  1. Configuração e validação de parâmetros de clique simples/duplo e botões do mouse.
  2. Gerenciamento de gravação de eventos de hardware em tempo real.
  3. Manipulação de diálogos modais de interface (`DialogService`).
  4. Execução de leitura e gravação síncrona de arquivos `.json` no disco (`MacroStorageService`).
  5. Formatação de logs textuais com `LocalTime`.
  6. Orquestração de animações e cálculos de CPS (`Timeline`).
  7. Descoberta e iteração de janelas ativas do sistema operacional via Panama FFM (`WindowService`).
  8. Captura interativa e reset de teclas de atalho globais.

### 2.2 O Híbrido Anti-Padrão dos Motores (`AutoClickerEngine` e `MacroEngine`)
As classes `AutoClickerEngine` e `MacroEngine` herdam de `AbstractEngine` e implementam um anti-padrão grave de ciclo de vida:
* Declaram uma instância estática privada `INSTANCE`.
* Expõem métodos estáticos públicos (`startAutoClicker()`, `stopAutoClicker()`, `setSetting()`, `setProgressListener()`, `startMacro()`).
* Ao mesmo tempo, fornecem construtores públicos e métodos de instância (`setFinishListener()`, `isRunning()`).
* No `AutoClickerController` (linhas 44 e 45), o programador instanciou:
  ```java
  private final AutoClickerEngine autoClickerEngine = new AutoClickerEngine();
  private final MacroEngine macroEngine = new MacroEngine();
  ```
  Porém, ao iniciar a execução (linha 350-351), o código chama métodos estáticos:
  ```java
  AutoClickerEngine.setSetting(autoClickerSetting);
  AutoClickerEngine.startAutoClicker();
  ```
  E para ouvir o término (linhas 196-198), chama o método de instância:
  ```java
  autoClickerEngine.setFinishListener(() -> Platform.runLater(this::handleAutoFinished));
  ```
  Para contornar a inconsistência de chamadas na instância vs chamadas na classe, o desenvolvedor sobrescreveu `setFinishListener` em `AutoClickerEngine`:
  ```java
  @Override
  public void setFinishListener(Runnable listener) {
      super.setFinishListener(listener);
      if (this != INSTANCE) {
          INSTANCE.setFinishListener(listener);
      }
  }
  ```
* **Consequência:** Estado global compartilhado, impossibilidade de rodar testes unitários paralelos, vazamento de listeners e dependência estática oculta que impede injeção de dependência limpa.

### 2.3 Bug Crítico Funcional: Perda das Configurações de Reprodução de Macro
Em `AutoClickerController.java`, nas linhas 582 a 603, há um bug funcional fatal:
```java
MacroPlaybackSettingDTO playback = criarMacroPlaybackSetting(); // Coleta repetições, infinito e intervalo
MacroSettingDTO macro = macroStorageService.load(selected);
...
macroEngine.setIterationListener(...);
macroEngine.setFinishListener(...);

MacroEngine.startMacro(macro); // <-- BUG: Executa a sobrecarga de 1 argumento!
```
A classe `MacroEngine` possui duas sobrecargas:
1. `startMacro(MacroSettingDTO macro)`: executa com `MacroPlaybackSettingDTO.once()` (apenas 1 vez).
2. `startMacro(MacroSettingDTO macro, MacroPlaybackSettingDTO playbackSetting)`: executa respeitando a repetição configurada.

Como o controller chama a versão de 1 argumento, **a configuração de repetição informada pelo usuário no formulário da aba Macro (infinito, 5x, 10x, intervalo entre repetições) é sumariamente descartada**. O usuário configura repetições na tela, a interface exibe mensagens de que vai repetir, mas o motor roda apenas uma única vez.

### 2.4 Threading, I/O e Falhas de Concorrência
1. **I/O Bloqueante na JavaFX Application Thread:**
   * Em `saveMacro()`, `refreshMacroList()` e `carregarDetalhesMacro()`, operações de leitura e escrita em disco via Jackson (`objectMapper.writeValue` e `readValue`) são executadas diretamente na thread de eventos do JavaFX. Se o disco apresentar lentidão, a interface congela por completo.
2. **Panama FFM X11 na JavaFX Application Thread:**
   * O método `refreshOpenWindows()` invoca `windowService.getOpenWindows()`, que faz chamadas nativas em C na biblioteca X11 (`libX11.so.6`) síncronas na thread gráfica.
3. **Condição de Corrida (Race Condition) em `MacroService`:**
   * Na gravação da macro (`captureMacro`), a lista `macroActionsTemp` é instanciada como um `java.util.ArrayList` comum (não thread-safe).
   * O `keyListener` e o `mouseListener` do JNativeHook rodam em threads distintas de background da biblioteca nativa.
   * Ambas as threads inserem elementos concorrentemente em `macroActionsTemp` via `add()` e iteram de trás para frente. Isso causa perda de eventos gravados e riscos iminentes de `ArrayIndexOutOfBoundsException` ou estado inconsistente.
4. **Falta de Sincronização em `HotkeyService`:**
   * O conjunto `currentlyPressedKeys` é um `HashSet` manipulado concorrentemente nas threads de eventos do JNativeHook sem nenhuma trava de sincronização (`synchronized` ou `ConcurrentHashMap.newKeySet()`).

### 2.5 Código Morto, Classes Órfãs e Nomenclatura Confusa
* **Classes Mortas:**
  * `com.example.autoclicker.config.JNativeHookConfig`: Classe marcada como `@Deprecated`, que não é instanciada nem referenciada em nenhum ponto funcional do projeto. Além disso, invoca `System.exit(1)` caso ocorra erro.
  * `com.example.autoclicker.service.GlobalHotkeyListener`: Classe marcada como `@Deprecated`, órfã, contendo métodos que misturam `KeyCode` do JavaFX com listeners do JNativeHook.
* **Nomenclatura e Tipagem:**
  * `MouseButton.getLabel()` retorna um `int` (a máscara de bits do AWT `InputEvent.BUTTON1_DOWN_MASK`), enquanto `getDisplayName()` retorna a descrição textual. Chamar uma máscara de bits de `label` gera extrema confusão.
  * `ClickType.getLabel()` retorna um `int` (a quantidade de cliques, 1 ou 2). Novamente, o nome `label` é inadequado para uma grandeza inteira.
  * `MacroActionSetting.java`: Possui getters duplicados com nomenclaturas distintas (`getxBefore()` e `getXBefore()`, `getyBefore()` e `getYBefore()`, `getxAfter()` e `getXAfter()`, `getyAfter()` e `getYAfter()`), criados provavelmente como paliativo para falhas de reflexão no Jackson.
  * `OperationalSystemConfig`: Trata-se de uma fábrica (`WindowServiceFactory`), mas foi nomeada como classe de configuração e colocada no pacote `config`.
  * `MacroStorageService.listAllSetings()`: Método público com erro ortográfico óbvio ("Setings"), mantido apenas para delegar para `listAllSettings()`.

---

## 3. Catálogo Detalhado de Problemas e Propostas de Refatoração

| ID | Arquivo / Localização | Classificação | Problema (1-2 frases) | Risco ou Custo | Refatoração Concreta Proposta |
|:---|:---|:---|:---|:---|:---|
| **P-01** | `AutoClickerController.java`<br>(L582-L603) | **Fatal Functional Bug** | O método `runSelectedMacro` calcula o objeto `MacroPlaybackSettingDTO playback`, mas chama `MacroEngine.startMacro(macro)` em vez de `MacroEngine.startMacro(macro, playback)`. | A funcionalidade central de repetição de macros não funciona: a macro sempre é executada uma única vez, desrespeitando os parâmetros definidos pelo usuário. | Alterar a chamada para `MacroEngine.startMacro(macro, playback)` e adicionar teste unitário de integração no motor garantindo múltiplas repetições. |
| **P-02** | `AbstractEngine.java`<br>(L14-L26, L46-L53)<br>`pom.xml` (L116) | **Fatal Functional Bug** | Acoplamento rígido com `java.awt.Robot` e AWT Toolkit, somado à flag `-Djava.awt.headless=false` no Surefire, explodindo com `UnsatisfiedLinkError` em ambientes Linux sem `libawt_xawt.so`. | Impede a execução de testes automatizados no Maven e em servidores de CI/CD, além de disparar cliques reais de mouse no desktop do desenvolvedor durante testes. | Criar a interface `InputSimulator` (com implementações `AwtRobotInputSimulator` e `VirtualInputSimulator` para testes). Injetar o simulador no motor e remover a dependência de AWT real em tempo de teste. |
| **P-03** | `MacroService.java`<br>(L29, L53, L82)<br>`HotkeyService.java` (L15, L48) | **Fatal Functional Bug** | Eventos nativos assíncronos de teclado e mouse do JNativeHook inserem e consultam dados concorrentemente em `ArrayList` e `HashSet` sem sincronização. | Condição de corrida resultando em `ConcurrentModificationException`, corrupção da gravação de ações de macro e teclas de atalho não detectadas. | Substituir por estruturas concorrentes thread-safe: `CopyOnWriteArrayList` ou `Collections.synchronizedList` em `MacroService`, e `ConcurrentHashMap.newKeySet()` em `HotkeyService`. |
| **P-04** | `AutoClickerController.java`<br>(L1-L669)<br>`autoClicker-view.fxml` | **Fatal Functional Bug** | Um único "God Controller" de 669 linhas gerencia duas abas complexas, manipulação de arquivos, chamadas FFM X11 e timers visuais. | Altíssimo custo de manutenção, impossibilidade de testar componentes isolados e elevado risco de regressão em qualquer ajuste visual. | Decompor o FXML em subcomponentes com `<fx:include>` (`auto-clicker-tab.fxml` e `macro-tab.fxml`), criando controllers específicos (`AutoClickerTabController`, `MacroTabController`) mediados por um `MainController`. |
| **P-05** | `AutoClickerEngine.java`<br>`MacroEngine.java`<br>(L8-L60) | **Fatal Functional Bug** | Padrão híbrido confuso entre Singleton privado, fachada com métodos estáticos de estado global e métodos de instância na mesma classe. | Inviabiliza paralelismo de testes, mascara o ciclo de vida dos motores, causa vazamento de listeners e impede a injeção limpa de dependências. | Eliminar métodos estáticos e a instância Singleton. Tornar as engines objetos normais de domínio gerenciados e injetados via construtor nos respectivos controllers. |
| **P-06** | `AutoClickerController.java`<br>(L517-L567, L644-L654) | **Fatal Functional Bug** | Chamadas bloqueantes de I/O de disco (`MacroStorageService`) e chamadas nativas X11 (`LinuxX11WindowService`) são executadas síncronas na thread da interface. | Congelamento e travamento da interface visual (UI freeze) caso o disco esteja ocupado ou o servidor gráfico demore a responder. | Executar as operações de persistência e enumeração de janelas em segundo plano usando `javafx.concurrent.Task` ou `CompletableFuture.runAsync()`, atualizando a UI via `Platform.runLater()`. |
| **P-07** | `MacroActionSetting.java`<br>(L69-L133) | **Minor Robustness Risk** | Getters e setters duplicados para as coordenadas de início e término (`getxBefore`/`getXBefore`, `getyBefore`/`getYBefore`, etc.). | Poluição do código, confusão no uso da API de domínio e potenciais anomalias de reflexão de atributos durante desserialização com Jackson. | Eliminar as versões com minúscula (`getxBefore`, etc.), padronizando para a convenção canônica JavaBean (`getXBefore`, `getYBefore`, `getXAfter`, `getYAfter`). |
| **P-08** | `AutoClickerEngine.java`<br>(L87-L133) | **Minor Robustness Risk** | Duplicação de código entre os métodos de clique contínuo (`autoClickerInfinite`) e de contagem limitada (`autoClickerCounts`). | Risco de inconsistência: se a lógica de cálculo de tempo ou compensação de deriva (drift compensation) for ajustada, a alteração precisa ser duplicada. | Unificar a lógica em um único loop condicional controlado por uma função predicativa `hasNextClick()`. |
| **P-09** | `JNativeHookConfig.java`<br>`GlobalHotkeyListener.java`<br>`MacroStorageService.java` (L46) | **Minor Robustness Risk** | Presença de classes marcadas como `@Deprecated` que não são referenciadas em lugar nenhum, e método com erro ortográfico (`listAllSetings`). | Acúmulo de débito técnico, confusão para desenvolvedores e manutenção de código morto. | Excluir definitivamente as duas classes depreciadas e remover o método delegado `listAllSetings()`. |
| **P-10** | `MouseButton.java`<br>`ClickType.java` | **Minor Robustness Risk** | O método `getLabel()` é utilizado para retornar inteiros que representam máscaras de bits AWT ou multiplicadores de clique. | Nomenclatura ambígua que contradiz o propósito do dado, prejudicando a clareza e auto-documentação do código. | Renomear `MouseButton.getLabel()` para `getMask()` e `ClickType.getLabel()` para `getClickMultiplier()`. |
| **P-11** | `pom.xml`<br>(L17-L29) | **Minor Robustness Risk** | Repositório JCenter (descontinuado) e repositório GitHub de terceiros declarados no POM. | Aumento do tempo de resolução de dependências no Maven e risco de falha de build se o JCenter retornar erro HTTP permanente. | Remover os blocos `<repository>` obsoletos, mantendo o download de dependências exclusivamente a partir do Maven Central. |
| **P-12** | `AbstractEngine.java`<br>`MacroEngine.java`<br>`MacroService.java` | **Minor Robustness Risk** | Tratamento de exceções com blocos `catch (Exception ignored) {}` vazios espalhados pelo código de limpeza de periféricos e remoção de listeners. | Falhas silenciosas que mascaram bugs de liberação de teclas/mouse travados no sistema operacional. | Substituir por logging explícito (`System.getLogger` ou `java.util.logging.Logger`) em nível DEBUG/WARNING, garantindo rastreabilidade. |

---

## 4. Proposta de Reorganização Estrutural de Pastas e Pacotes

A estrutura proposta respeita os padrões de mercado amplamente consolidados no ecossistema Java/JavaFX:
* **Princípio da Separação por Responsabilidade Arquitetural (Separation of Concerns):** O domínio de negócio não conhece bibliotecas de UI (JavaFX) nem drivers nativos de hardware (`java.awt.Robot`, `GlobalScreen`).
* **Convenção Canônica de Pacotes Java:** Eliminação do pacote com acrônimo em caixa alta (`DTO` -> `dto`).
* **Modularização Limpa de Sub-interfaces FXML:** Decomposição do FXML gigante em abas independentes (`auto-clicker-tab.fxml` e `macro-tab.fxml`).

### 4.1 Árvore Atual vs Árvore Proposta

#### Estrutura Atual:
```text
src/main/java/
├── module-info.java
└── com/example/autoclicker/
    ├── AutoClickerApplication.java
    ├── Launcher.java
    ├── DTO/                               <-- Violação de convenção (caixa alta)
    │   ├── MacroDialogDTO.java
    │   ├── MacroPlaybackSettingDTO.java
    │   ├── MacroSettingDTO.java
    │   └── WindowInfoDTO.java
    ├── config/
    │   ├── JNativeHookConfig.java         <-- Código morto (@Deprecated)
    │   ├── JacksonConfig.java
    │   ├── NativeKeyConfig.java
    │   └── OperationalSystemConfig.java   <-- Na verdade é uma Factory de WindowService
    ├── controller/
    │   └── AutoClickerController.java     <-- 669 linhas (God Controller de ambas as abas)
    ├── domain/
    │   ├── AbstractEngine.java            <-- Acoplado a java.awt.Robot
    │   ├── AutoClickerEngine.java         <-- Singleton híbrido com métodos estáticos
    │   ├── AutoClickerSetting.java
    │   ├── MacroActionSetting.java        <-- Getters/setters duplicados
    │   └── MacroEngine.java               <-- Singleton híbrido com métodos estáticos
    ├── enums/
    │   ├── ActionType.java
    │   ├── ClickType.java
    │   ├── MouseButton.java
    │   └── StatusState.java
    └── service/
        ├── DialogService.java
        ├── GlobalHotkeyListener.java      <-- Código morto (@Deprecated)
        ├── HotkeyService.java
        ├── LinuxX11WindowService.java
        ├── MacroService.java
        ├── MacroStorageService.java
        ├── WindowService.java
        └── WindowsWindowService.java
```

#### Estrutura Proposta (Alinhada a Padrões de Mercado):
```text
src/main/java/
├── module-info.java
└── com/example/autoclicker/
    ├── AutoClickerApplication.java
    ├── Launcher.java
    │
    ├── domain/                            <-- Regras de negócio puras (sem JavaFX, sem AWT)
    │   ├── model/
    │   │   ├── AutoClickerSetting.java
    │   │   ├── MacroActionSetting.java
    │   │   ├── ActionType.java
    │   │   ├── ClickType.java
    │   │   ├── MouseButton.java
    │   │   └── StatusState.java
    │   └── engine/
    │       ├── AbstractEngine.java        <-- Recebe InputSimulator via construtor
    │       ├── AutoClickerEngine.java     <-- Instância pura de domínio (sem métodos estáticos)
    │       └── MacroEngine.java           <-- Instância pura de domínio (sem métodos estáticos)
    │
    ├── dto/                               <-- Records e DTOs imutáveis (pacote minúsculo)
    │   ├── MacroDialogDTO.java
    │   ├── MacroPlaybackSettingDTO.java
    │   ├── MacroSettingDTO.java
    │   └── WindowInfoDTO.java
    │
    ├── input/                             <-- Abstrações e drivers de entrada do SO
    │   ├── simulator/
    │   │   ├── InputSimulator.java        <-- Interface que permite mocks em testes
    │   │   ├── AwtRobotInputSimulator.java
    │   │   └── VirtualInputSimulator.java <-- Implementação para testes headless
    │   ├── hotkey/
    │   │   ├── HotkeyService.java
    │   │   └── NativeKeyConfig.java
    │   └── window/
    │       ├── WindowService.java
    │       ├── WindowServiceFactory.java  <-- Antigo OperationalSystemConfig
    │       ├── LinuxX11WindowService.java
    │       └── WindowsWindowService.java
    │
    ├── storage/                           <-- Persistência de dados local
    │   ├── MacroStorageService.java
    │   └── JacksonConfig.java
    │
    └── ui/                                <-- Camada visual JavaFX
        ├── controller/
        │   ├── MainController.java        <-- Coordena as abas e atalho global
        │   ├── AutoClickerTabController.java
        │   └── MacroTabController.java
        └── dialog/
            └── DialogService.java
```

### 4.2 Mapeamento de Migração (De -> Para)

| Classe / Arquivo Original | Destino Proposto | Justificativa do Padrão de Mercado |
|:---|:---|:---|
| `DTO.*` | `dto.*` | Convenção canônica Java (JLS). Nomes de pacotes devem ser integralmente minúsculos. |
| `domain.AbstractEngine`, `AutoClickerEngine`, `MacroEngine` | `domain.engine.*` | Separação entre entidades de configuração de dados (`model`) e executores de domínio (`engine`). |
| `domain.AutoClickerSetting`, `MacroActionSetting`, `enums.*` | `domain.model.*` | Coesão de domínio: enums de tipo de clique e botão pertencem ao modelo de dados do AutoClicker. |
| `service.HotkeyService`, `config.NativeKeyConfig` | `input.hotkey.*` | Agrupamento por capacidade de infraestrutura de entrada (Input Subsystem). |
| `service.WindowService`, `WindowsWindowService`, `LinuxX11WindowService` | `input.window.*` | Agrupamento de serviços de inspeção de janelas sob subsistema de entrada/SO. |
| `config.OperationalSystemConfig` | `input.window.WindowServiceFactory` | Nomenclatura explícita para o padrão **Factory Method** que a classe de fato implementa. |
| `service.MacroStorageService`, `config.JacksonConfig` | `storage.*` | Separação da camada de persistência em disco em relação aos serviços de domínio. |
| `service.DialogService` | `ui.dialog.*` | Classes que constroem nós gráficos JavaFX (`GridPane`, `DialogPane`) pertencem à camada `ui`. |
| `controller.AutoClickerController` | `ui.controller.MainController`, `AutoClickerTabController`, `MacroTabController` | Padrão **Composite View / Controller por Tela**: cada aba FXML possui seu controller independente. |
| `config.JNativeHookConfig`, `service.GlobalHotkeyListener` | *(Exclusão Definitiva)* | Eliminação de código morto, classes órfãs e APIs obsoletas. |
| `autoClicker-view.fxml` | `main-view.fxml`, `auto-clicker-tab.fxml`, `macro-tab.fxml` | Modularização de visões FXML reutilizáveis e testáveis com `<fx:include>`. |

### 4.3 Análise de Trade-offs da Reorganização

#### O que se ganha:
1. **Testabilidade Total:** O domínio pode ser 100% testado sem instanciar JavaFX nem carregar bibliotecas nativas de AWT X11, utilizando a nova interface `InputSimulator`.
2. **Manutenibilidade Acelerada:** Alterações na tela de Macro não impactam a tela do AutoClicker, pois os controllers e FXMLs passam a ser independentes.
3. **Resiliência a Falhas de Concorrência:** As coleções concorrentes e o isolamento de threads em `storage` impedem travamentos de interface gráfica.
4. **Conformidade com o Ecossistema Java:** Código padronizado de acordo com o Java Language Specification e as melhores práticas da comunidade JavaFX.

#### O que se perde / Custos de Migração:
1. **Refatoração de Imports:** Todas as classes terão seus comandos `import` alterados devido à mudança de pacotes.
2. **Ajustes Obrigatórios no `module-info.java`:**
   * Atualizar `opens ... to javafx.fxml` para `com.example.autoclicker.ui.controller`.
   * Atualizar `opens ... to com.fasterxml.jackson.databind` para `com.example.autoclicker.dto` e `com.example.autoclicker.domain.model`.
   * Atualizar os blocos `exports`.
3. **Ajustes nos FXMLs:** As tags `fx:controller` de cada arquivo FXML devem apontar para os novos nomes qualificados das classes.
4. **Ajustes no `javafx-maven-plugin`:** Verificar se os caminhos de classe principal continuam apontando para `com.example.autoclicker.AutoClickerApplication`.

---

## 5. Plano de Ação, Roteiro de Execução e Matriz de Riscos

### 5.1 Os Cinco Problemas de Maior Impacto

1. **Acoplamento de `java.awt.Robot` e AWT Toolkit:** Impede testes no Maven/CI e dispara cliques reais no sistema do desenvolvedor.
2. **Bug Crítico na Reprodução de Macro:** O controller descarta a configuração `MacroPlaybackSettingDTO`, impedindo repetições de macros.
3. **Condição de Corrida na Gravação de Macros e Atalhos:** `MacroService` e `HotkeyService` utilizam coleções não thread-safe sob eventos nativos assíncronos.
4. **"God Controller" de 669 Linhas:** Monólito que une duas abas, viola o SRP e executa I/O de disco e Panama X11 na thread de interface.
5. **Anti-Padrão Singleton/Estático nos Motores:** Impede paralelismo de testes, mascara o ciclo de vida e impossibilita injeção limpa de dependências.

---

### 5.2 Ordem Sugerida de Execução das Refatorações

A execução deve seguir uma ordem estritamente defensiva, **começando pela recuperação e expansão dos testes automatizados**:

```
[FASE 0: Recuperação da Suíte de Testes]
   ├── 1. Criar interface InputSimulator e implementação VirtualInputSimulator
   ├── 2. Desacoplar AbstractEngine de java.awt.Robot concreto
   ├── 3. Ajustar AutoClickerEngineTest para usar VirtualInputSimulator
   ├── 4. Remover / neutralizar <argLine>-Djava.awt.headless=false no pom.xml
   └── 5. Obter ./mvnw test 100% verde
               │
               v
[FASE 1: Testes de Caracterização (Golden Master)]
   ├── 1. Adicionar testes unitários para MacroEngine (respeito a loops e delays)
   ├── 2. Adicionar testes para MacroStorageService (salvamento/leitura JSON)
   └── 3. Adicionar testes de concorrência para MacroService
               │
               v
[FASE 2: Correções de Bugs Críticos e Segurança de Threads]
   ├── 1. Corrigir a chamada de MacroEngine.startMacro(macro, playback) no Controller
   ├── 2. Trocar coleções por thread-safe (CopyOnWriteArrayList e ConcurrentHashMap)
   └── 3. Mover I/O de disco e Panama FFM para background via CompletableFuture
               │
               v
[FASE 3: Limpeza de Código Morto e Melhorias de Nomenclatura]
   ├── 1. Deletar JNativeHookConfig e GlobalHotkeyListener
   ├── 2. Remover métodos duplicados em MacroActionSetting
   ├── 3. Renomear métodos nos Enums (getLabel -> getMask / getClickMultiplier)
   └── 4. Limpar repositórios obsoletos no pom.xml
               │
               v
[FASE 4: Reorganização Estrutural de Pastas e Decomposição de Telas]
   ├── 1. Decompor autoClicker-view.fxml em abas com controllers separados
   ├── 2. Eliminar métodos estáticos e Singleton em AutoClickerEngine e MacroEngine
   ├── 3. Mover classes para os novos pacotes (renomeando DTO -> dto)
   └── 4. Atualizar module-info.java e referências FXML
```

---

### 5.3 Classificação de Risco das Mudanças

#### A. Mudanças Seguras para Aplicação Imediata (Baixo Risco)
* **Correção do Bug de Repetição da Macro:** Alterar `MacroEngine.startMacro(macro)` para `MacroEngine.startMacro(macro, playback)` em `AutoClickerController`. Risco nulo de quebra estrutural e ganho funcional imediato.
* **Remoção de Código Morto:** Deletar `JNativeHookConfig.java`, `GlobalHotkeyListener.java` e o método delegado `listAllSetings()`. Essas classes já estão sem referências ativas no projeto.
* **Limpeza do `pom.xml`:** Remoção do repositório descontinuado JCenter. Acelera a resolução de dependências no Maven sem afetar o código-fonte.
* **Tornar Coleções Thread-Safe:** Substituição de `new ArrayList<>()` por `new CopyOnWriteArrayList<>()` em `MacroService.java` e conjunto concorrente em `HotkeyService.java`. Elimina condições de corrida sem alterar a assinatura pública dos métodos.
* **Eliminação de Getters Duplicados em `MacroActionSetting`:** Remover as versões redundantes com inicial minúscula (`getxBefore()`, etc.), mantendo a versão canônica JavaBean.

#### B. Mudanças que Exigem Cuidado e Testes Prévios (Alto Risco)
* **Introdução da Abstração `InputSimulator`:** Modifica a hierarquia de `AbstractEngine`, `AutoClickerEngine` e `MacroEngine`. Exige validação prévia de testes de caracterização para garantir que nenhum comportamento de clique ou sincronia fina (`sleepUntil`) seja alterado.
* **Decomposição do FXML e do Controller:** Separar `AutoClickerController` em sub-controladores requer mexer na hierarquia do FXML (`<fx:include>`), nos atributos `fx:id` e na fiação de dependências entre controllers. Exige testes cuidadosos de inicialização do JavaFX Toolkit.
* **Reorganização de Pacotes e Renomeação de `DTO`:** Quebra de compatibilidade em 100% dos arquivos fonte (imports) e no arquivo `module-info.java`. Deve ser executada com auxílio de refatoração automatizada da IDE e suíte de testes 100% verde antes e depois da migração.
* **Eliminação dos Métodos Estáticos nos Motores:** Exige que a fábrica do controlador (`controllerFactory` no `AutoClickerApplication`) passe a instanciar e injetar os motores explicitamente, mudando os pontos de entrada de eventos de hotkey.

---

## 6. Aspectos Não Avaliados por Falta de Contexto

Para garantir total transparência e rigor de engenharia, registram-se os seguintes aspectos que não puderam ser plenamente validados na íntegra:

1. **Comportamento do JNativeHook sob Wayland (Linux Moderno):**
   * O sistema operacional utilizado no ambiente Linux executa rotinas baseadas em X11 (como demonstrado pelo `LinuxX11WindowService` buscando `libX11.so.6`). Sob sessões modernas baseadas no protocolo **Wayland**, compositores gráficos (GNOME/Mutter, KDE/KWin) bloqueiam por padrão a captura global de teclado e a injeção de eventos de clique sintéticos por razões de segurança. Não há como garantir que o aplicativo funcione nativamente em Wayland sem testes manuais dedicados ou sem forçar `GDK_BACKEND=x11` / `XWayland`.
2. **Implementação de `WindowsWindowService`:**
   * A classe `WindowsWindowService` encontra-se como um esqueleto vazio (`return List.of();`), com anotação indicando desenvolvimento futuro via User32 (`EnumWindows`). Não há implementação funcional para listar janelas no ecossistema Microsoft Windows neste momento.
3. **Calibração de Baixa Latência (`sleepUntil` com Spin-Wait):**
   * O método `sleepUntil` em `AbstractEngine` utiliza uma constante de 2 ms (`SPIN_THRESHOLD_NANOS = 2_000_000L`) com `Thread.onSpinWait()`. A eficácia real dessa compensação de deriva temporal depende profundamente do escalonador do kernel do sistema operacional e da resolução do timer do hardware. O comportamento em taxa máxima (1000 CPS / 1 ms) sob processadores com múltiplos núcleos de eficiência energética (ex.: Intel hybrid architecture ou ARM) não foi avaliado em benchmark sintético.
