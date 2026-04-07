# AutoClicker (JavaFX)

Projeto desktop em JavaFX para automatizar cliques de mouse com configuracao de intervalo, atraso inicial, modo infinito/limitado, tipo de clique e hotkey para iniciar/parar.

## Objetivo do projeto

Este projeto foi organizado para separar responsabilidades:

- UI e eventos de tela no controller (`AutoClickerController`)
- regras de configuracao e validacao no dominio (`AutoClickerSetting`)
- execucao do clique em thread dedicada (`AutoClickerEngine`)

Assim, voce pode evoluir backend/automacao sem reescrever o layout.

## Funcionalidades atuais

- Layout JavaFX em FXML (`autoClicker-view.fxml`)
- Campos de configuracao:
  - Intervalo entre cliques (ms)
  - Atraso inicial (ms)
  - Quantidade de cliques
  - Modo infinito
  - Tipo de clique (`SIMPLES`, `DUPLO`)
  - Botao do mouse (`ESQUERDO`, `DIREITO`, `MEIO`)
- Captura de hotkey pela propria tela
- Botao de iniciar/parar com mudanca visual de estado
- Execucao de clique em thread separada (evita travar a UI)
- Validacoes de configuracao antes de iniciar

## Limitacao importante (hotkey)

A hotkey atual depende de eventos da `Scene` JavaFX.

- Funciona quando a janela esta em foco
- Nao e hotkey global do sistema operacional

Existe uma classe `GlobalHotkeyListener`, mas na versao atual ela funciona como helper em memoria, nao como hook nativo global do Windows.

## Stack tecnica

- Java 25 (Temurin)
- JavaFX 21.0.6
- Maven Wrapper (`mvnw.cmd`)
- FXML + controller JavaFX

## Estrutura do projeto

```text
src/main/java/
  module-info.java
  com/example/autoclicker/
    AutoClickerApplication.java
    Launcher.java
    Controller/
      AutoClickerController.java
    domain/
      AutoClickerSetting.java
      AutoClickerEngine.java
    service/
      GlobalHotkeyListener.java

src/main/resources/
  com/example/autoclicker/
    autoClicker-view.fxml
```

## Arquitetura (resumo)

### `AutoClickerController`

Responsavel por:

- carregar valores padrao da UI
- ler formulario
- capturar hotkey
- montar `AutoClickerSetting`
- exibir erros no `statusArea`
- acionar `AutoClickerEngine.startAutoClicker()` e `stopAutoClicker()`

### `AutoClickerSetting`

Contem:

- enums `ClickType` e `MouseButton`
- dados da configuracao (`interval`, `delay`, `clickCount`, etc.)
- validacao de regras via `validateSetting()`

### `AutoClickerEngine`

Contem a execucao de clique:

- usa `java.awt.Robot`
- executa em thread daemon (`auto-clicker-worker`)
- respeita delay inicial e intervalo
- suporta cliques infinitos e limitados
- permite parada via `stopAutoClicker()`

## Regras de validacao

Implementadas em `AutoClickerSetting.validateSetting()`:

- intervalo deve ser maior que 0
- atraso inicial deve ser maior ou igual a 0
- se `infiniteClick == false`, `clickCount` deve ser maior que 0
- se `infiniteClick == true`, `clickCount` deve ser 0
- tipo de clique obrigatorio
- botao do mouse obrigatorio

Quando alguma regra falha, o controller mostra os erros no `statusArea` e nao inicia o motor de cliques.

## Pre-requisitos

- Windows
- JDK 25 instalado
- `JAVA_HOME` configurado
- internet para baixar dependencias Maven (primeira execucao)

## Configurando JAVA_HOME (Windows)

Exemplo de caminho de JDK (ajuste conforme sua maquina):

`C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot`

PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot", "User")
```

Abra um novo terminal e valide:

```powershell
$env:JAVA_HOME
java -version
```

## Como executar

No PowerShell, dentro da pasta do projeto:

```powershell
Set-Location "C:\Users\gusta\OneDrive\Documentos\VISUAL_CODE\AutoClicker"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

Alternativa: executar `run.bat` na raiz do projeto.

## Como usar a aplicacao

1. Preencha intervalo, atraso e (se necessario) quantidade
2. Escolha tipo de clique e botao do mouse
3. (Opcional) marque `Infinito`
4. Clique em `Selecionar Tecla de Atalho` e pressione a combinacao
5. Clique em `Iniciar`
6. Use `Parar` para interromper

## Troubleshooting

### 1) `JAVA_HOME not found in your environment`

Configure `JAVA_HOME` e abra novo terminal antes de rodar Maven.

### 2) `Nao foi possivel localizar nem carregar a classe principal ... Launcher`

Normalmente ocorre quando `target/classes` esta desatualizado.

```powershell
.\mvnw.cmd clean compile
```

### 3) `InvalidModuleDescriptorException: Package ... not found in module`

Tambem costuma indicar build quebrado/parcial.

```powershell
.\mvnw.cmd clean
.\mvnw.cmd compile
```

### 4) Warnings de `Restricted methods`/`Unsafe`

Sao avisos comuns com Java mais novo e dependencias JavaFX/Maven internas.
Nao necessariamente impedem execucao.

### 5) Hotkey nao funciona fora da janela

Comportamento esperado na implementacao atual (hotkey baseada em eventos de foco da UI).

## Melhorias recomendadas

- Hotkey global real (hook nativo por SO)
- Persistencia de configuracao (JSON/properties)
- Logs estruturados
- Testes unitarios para validacoes
- Tratamento de erros de captura de mouse/teclado com feedback mais detalhado

## Observacoes finais

- O projeto esta preparado para evolucao incremental.
- O layout pode continuar separado da logica sem impacto no engine.
- Para evoluir backend, concentre novas regras em `domain`/`service` e mantenha o controller leve.
