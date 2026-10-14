# AutoClicker & Macro Pro (JavaFX) - Versão 2.0

Aplicativo desktop modular de alta performance desenvolvido em **Java 25 LTS** e **JavaFX 21**, projetado para automação de cliques e gravação/reprodução de macros com suporte a **Hotkeys Globais** (JNativeHook) e arquitetura orientada a padrões de projeto de mercado (Clean Architecture, Strategy, Template Method e SOLID).

---

## 🚀 Novidades da Versão 2.0

A versão **2.0** traz uma reformulação arquitetural profunda e recursos profissionais de automação:

- **Sistema Avançado de Gravação e Reprodução de Macros**:
  - Gravação global em tempo real (cliques simples, cliques duplos, arrastos de mouse *drag and drop* e teclas).
  - Tecla de atalho global **`[ESC]`** para parada emergencial instantânea, mesmo com o aplicativo desfocado ou minimizado.
  - Repetição flexível: modo de execução única, quantidade finita de repetições ou ciclo infinito, com intervalos configuráveis em milissegundos.
  - Visualização em tempo real das ações gravadas com linha do tempo formatada.
- **Persistência Estruturada e Retrocompatibilidade (JSON)**:
  - Armazenamento em disco na pasta `storage/macros/` usando Jackson Databind 2.18 com suporte total a esquemas legados via `@JsonAlias` e `@JsonIgnoreProperties`.
  - Carregamento e gravação de arquivos JSON 100% assíncronos via `CompletableFuture`, sem travamento da interface gráfica.
- **Camada de Abstração de Hardware (`InputSimulator`)**:
  - Desacoplamento completo do `java.awt.Robot` concreto através da interface [`InputSimulator`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/input/simulator/InputSimulator.java).
  - Implementação de produção: [`AwtRobotInputSimulator`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/input/simulator/AwtRobotInputSimulator.java).
  - Implementação de testes em memória: [`VirtualInputSimulator`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/input/simulator/VirtualInputSimulator.java) para testes determinísticos sem mexer no cursor do desenvolvedor.
- **Decomposição do "God Controller" (Padrão MVC Modular com `<fx:include>`)**:
  - Divisão da interface em abas autônomas: [`AutoClickerTabController`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/ui/controller/AutoClickerTabController.java) e [`MacroTabController`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/ui/controller/MacroTabController.java), orquestradas pelo [`MainController`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/ui/controller/MainController.java).
  - Injeção de dependências limpa através da `ControllerFactory` no [`AutoClickerApplication`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/AutoClickerApplication.java).
- **Thread-Safety e Concorrência**:
  - Coleções compartilhadas protegidas com coleções sincronizadas e sets concorrentes em [`MacroService`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/input/macro/MacroService.java) e [`HotkeyService`](file:///home/guest4hu/Documents/Projects/Java/AutoClicker-JAVA-/src/main/java/com/example/autoclicker/input/hotkey/HotkeyService.java).

---

## ⚡ Recursos do AutoClicker

- **Presets Rápidos de Velocidade**:
  - Botões de um clique: `1 ms`, `10 ms`, `50 ms`, `100 ms`, `500 ms` e `1000 ms`.
  - Estimativa imediata da taxa de CPS (Cliques por Segundo).
- **Configuração de Periféricos**:
  - Botões suportados: Esquerdo, Direito e Meio (Scroll).
  - Tipo de clique: Simples ou Duplo.
  - Modos de execução: Contínuo (Infinito) ou com contagem limitada e encerramento automático.
- **Métricas e Monitoramento ao Vivo**:
  - Estatísticas de cliques executados, tempo decorrido e CPS real medido.
- **Atalhos Globais**:
  - Atalho padrão `F6` configurável para qualquer tecla do teclado.

---

## 🏗️ Arquitetura do Projeto

O projeto segue rigorosamente o padrão canônico Java/JavaFX com modularização de pacotes:

```text
src/
├── main/
│   ├── java/
│   │   ├── module-info.java
│   │   └── com/example/autoclicker/
│   │       ├── AutoClickerApplication.java      # Bootstrap JavaFX e Injeção de Dependências
│   │       ├── Launcher.java                    # Entry point para compatibilidade de empacotamento
│   │       ├── domain/
│   │       │   ├── engine/                      # Motores de execução assíncrona (Template Method)
│   │       │   │   ├── AbstractEngine.java
│   │       │   │   ├── AutoClickerEngine.java
│   │       │   │   └── MacroEngine.java
│   │       │   └── model/                       # Modelos de domínio e regras de negócio
│   │       │       ├── ActionType.java
│   │       │       ├── AutoClickerSetting.java
│   │       │       ├── ClickType.java
│   │       │       ├── MacroActionSetting.java
│   │       │       ├── MouseButton.java
│   │       │       └── StatusState.java
│   │       ├── dto/                             # Data Transfer Objects serializáveis (JavaBeans/Records)
│   │       │   ├── MacroDialogDTO.java
│   │       │   ├── MacroPlaybackSettingDTO.java
│   │       │   ├── MacroSettingDTO.java
│   │       │   └── WindowInfoDTO.java
│   │       ├── input/
│   │       │   ├── hotkey/                      # Gestão de atalhos globais (JNativeHook)
│   │       │   │   └── HotkeyService.java
│   │       │   ├── macro/                       # Captura de eventos do usuário em tempo real
│   │       │   │   └── MacroService.java
│   │       │   ├── simulator/                   # Abstração de hardware de entrada (Strategy)
│   │       │   │   ├── AwtRobotInputSimulator.java
│   │       │   │   ├── InputSimulator.java
│   │       │   │   └── VirtualInputSimulator.java
│   │       │   └── window/                      # Descoberta de janelas nativas (Panama FFM / OS)
│   │       │       ├── LinuxX11WindowService.java
│   │       │       ├── WindowService.java
│   │       │       ├── WindowServiceFactory.java
│   │       │       └── WindowsWindowService.java
│   │       ├── storage/                         # Persistência em disco via Jackson
│   │       │   ├── JacksonConfig.java
│   │       │   └── MacroStorageService.java
│   │       └── ui/                              # Apresentação visual JavaFX
│   │           ├── controller/                  # Sub-controladores modulares
│   │           │   ├── AutoClickerTabController.java
│   │           │   ├── MacroTabController.java
│   │           │   └── MainController.java
│   │           └── dialog/                      # Diálogos e modais
│   │               └── DialogService.java
│   └── resources/
│       └── com/example/autoclicker/
│           ├── auto-clicker-tab.fxml            # Aba do AutoClicker
│           ├── macro-tab.fxml                   # Aba do Gravador/Reprodutor de Macros
│           ├── main-view.fxml                   # Layout principal com TabPane e <fx:include>
│           ├── autoClicker-view.fxml            # FXML compatível com o Launcher original
│           ├── styles.css                       # Tema Dark Mode personalizado
│           └── icon.png                         # Ícone da aplicação
└── test/
    └── java/com/example/autoclicker/            # 12 suítes de testes unitários automatizados (38 testes)
        ├── AutoClickerEngineTest.java
        ├── AutoClickerSettingTest.java
        ├── FXMLValidationTest.java
        ├── HotkeyServiceTest.java
        ├── MacroActionSettingTest.java
        ├── MacroEngineTest.java
        ├── MacroPlaybackSettingDTOTest.java
        ├── MacroServiceTest.java
        ├── MacroStorageServiceTest.java
        ├── VirtualInputSimulatorTest.java
        ├── WindowInfoDTOTest.java
        └── WindowServiceFactoryTest.java
```

---

## 🛠️ Tecnologias e Dependências

- **Linguagem**: Java 25 LTS (OpenJDK).
- **Interface Gráfica**: OpenJFX 21.0.6 (JavaFX Controls e FXML).
- **Captura Global**: JNativeHook 2.2.2.
- **Serialização**: Jackson Databind 2.18.2 + Jackson JSR-310 (Java Time).
- **Testes**: JUnit 5.12.1 + Maven Surefire 3.2.5.

---

## 🚀 Como Executar

### Pré-requisitos
- **JDK 21+** (recomendado Java 25).
- **Maven 3.8+** (ou o wrapper `./mvnw` incluso).

### Linux (Sessão X11 / X.Org recomendada)
```bash
./mvnw clean compile
./mvnw javafx:run
```

### Windows (PowerShell / CMD)
```cmd
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

---

## 🧪 Testes Automatizados

A suíte conta com **38 testes unitários** automatizados e isolados via simulador virtual em memória (executáveis em ambientes headless ou servidores de integração contínua CI/CD):

```bash
./mvnw clean test
```

> **Resultado:** `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0` — **BUILD SUCCESS**.

---

## 🎮 Guia de Uso

### 1. AutoClicker
1. Acesse a aba **AutoClicker**.
2. Escolha o botão do mouse (Esquerdo, Direito ou Meio) e o tipo de clique (Simples ou Duplo).
3. Defina o intervalo em milissegundos (ex.: `10 ms`, `100 ms`).
4. Selecione se deseja cliques contínuos (infinito) ou uma quantidade fixa.
5. Inicie com **`▶ Iniciar (F6)`** ou pressione **`F6`** no teclado. Pressione **`F6`** novamente para parar.

### 2. Gravação e Reprodução de Macros
1. Acesse a aba **Macro**.
2. Clique em **`Gravar Macro (ESC para parar)`**.
3. Realize seus cliques, arrastos ou digitações em qualquer janela do computador.
4. Pressione a tecla **`[ESC]`** para encerrar a gravação.
5. Clique em **`Salvar Macro...`** para persistir em arquivo JSON com nome e descrição.
6. Selecione a macro na lista, ajuste o número de repetições e o intervalo entre execuções.
7. Clique em **`Executar Macro Selecionada`**. Para interromper a qualquer momento, aperte **`[ESC]`**.
