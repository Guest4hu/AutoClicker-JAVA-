# AutoClicker (JavaFX)

Aplicativo desktop desenvolvido em **JavaFX** com suporte a **Hotkeys Globais** (JNativeHook) e automação de cliques via `java.awt.Robot`.

---

## Destaques de UI / UX

- **Tema Preto e Vermelho**: Visual limpo, escuro e direto, com foco em ergonomia e utilidade para jogos e automações.
- **Indicador de Status**:
  - Badge no topo indicando os estados: `PRONTO`, `ATIVO` (vermelho) e `GRAVANDO`.
- **Atalho Global (F6)**:
  - Atalho padrão `F6` ativo em qualquer jogo ou aplicativo.
  - Tecla interativa configurável na interface.
- **Velocidade e Presets**:
  - Exibição de CPS conforme o intervalo digitado.
  - Botões rápidos: `1 ms`, `10 ms`, `50 ms`, `100 ms`, `500 ms` e `1000 ms`.
- **Estatísticas**:
  - Contagem de cliques em tempo real, tempo decorrido e taxa de CPS.
- **Modos de Execução**:
  - Suporte a cliques contínuos (infinito) ou quantidade fixa com encerramento automático.
- **Validação de Entrada**:
  - Campos numéricos protegidos contra entrada de caracteres inválidos.

---

## 🏗️ Arquitetura do Projeto

```text
src/
├── main/
│   ├── java/
│   │   ├── module-info.java
│   │   └── com/example/autoclicker/
│   │       ├── AutoClickerApplication.java  # Bootstrap, ícone da janela e ciclo de vida
│   │       ├── Launcher.java                # Ponto de entrada padrão
│   │       ├── controller/
│   │       │   └── AutoClickerController.java # Gerenciamento da UI, timers e hotkeys
│   │       ├── domain/
│   │       │   ├── AutoClickerEngine.java   # Motor de cliques em background com listeners
│   │       │   └── AutoClickerSetting.java  # Regras de negócio, validação e Enums
│   │       ├── service/
│   │       │   ├── HotkeyService.java       # Integração com JNativeHook e captura de teclas
│   │       │   └── GlobalHotkeyListener.java
│   │       └── config/
│   │           └── JNativeHookConfig.java
│   └── resources/
│       └── com/example/autoclicker/
│           ├── autoClicker-view.fxml        # Layout moderno responsivo
│           ├── styles.css                   # Design System Dark Mode
│           └── icon.png                     # Ícone do aplicativo em alta resolução
└── test/
    └── java/com/example/autoclicker/
        ├── AutoClickerSettingTest.java      # Testes unitários das regras de negócio
        └── FXMLValidationTest.java          # Validação e testes do toolkit JavaFX
```

---

## 🚀 Como Executar

### Pré-requisitos
- **JDK 21+** (recomendado Java 25)
- **Maven 3.8+** (ou o próprio `mvnw` incluído no projeto)

### Linux / macOS
```bash
./mvnw clean compile
./mvnw javafx:run
```

### Windows (PowerShell / CMD)
```cmd
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```
*(Ou execute `run.bat` na raiz)*

---

## 🧪 Testes Automatizados

Para executar a suite de testes automatizados com o JUnit 5:

```bash
./mvnw test
```

---

## 🎮 Como Usar

1. **Defina a Velocidade**:
   - Digite o intervalo em milissegundos ou clique em um dos chips de presets rápidos (`10 ms`, `50 ms`, etc.).
2. **Configure o Mouse**:
   - Selecione o botão do mouse (Esquerdo, Direito ou Meio) e o tipo de clique (Simples ou Duplo).
3. **Escolha o Ciclo**:
   - Marque **Repetição Contínua (Infinito)** ou informe a quantidade exata de cliques.
4. **Inicie o AutoClicker**:
   - Clique em **`▶ INICIAR [F6]`** ou simplesmente pressione a tecla **`F6`** no teclado em qualquer momento/tela.
5. **Para Parar**:
   - Pressione **`F6`** novamente ou clique em **`■ PARAR [F6]`**.
