package com.nightout;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

import com.nightout.WindowsThemeManager.Theme;

//TODO: melhorar essa classe, que esta muito grande e com muita responsabilidade.
//  Talvez separar em classes menores, como TrayIconManager, ThemeMenuManager, etc.


/** Ponto de entrada: fica residente na bandeja do Windows trocando o tema pelo sol da cidade. */
public final class App {

    private static TrayIcon iconeBandeja;
    private static MenuItem statusItem;
    private static AppConfig config;
    private static ThemeScheduler scheduler;
    private static final Map<ThemeMode, CheckboxMenuItem> opcoesDeTema = new EnumMap<>(ThemeMode.class);

    private static String LABEL_VERIFICAR_AGORA = "Verificar agora";

    public static void main(String[] args) {
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null,
                    "Este sistema nao suporta bandeja do Windows (SystemTray). NightOut nao pode iniciar.",
                    "NightOut", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        config = AppConfig.load();

        if (!config.isCityConfigured()) {
            CityResult picked = SettingsDialog.showAndSelect(config.displayName());
            if (picked == null) {
                Logger.log("Nenhuma cidade selecionada na primeira execucao; encerrando.");
                return;
            }
            applyCity(picked);
        }

        scheduler = new ThemeScheduler(config, App::onThemeApplied);

        try {
            criarIconeDaBandeja();
        } catch (AWTException e) {
            Logger.log("Falha ao criar icone na bandeja: " + e);
            return;
        }

        scheduler.start();
    }

    private static void criarIconeDaBandeja() throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();

        PopupMenu popup = new PopupMenu();
        popup.add(criarStatusCidade());
        popup.addSeparator();
        popup.add(criarMenuTema());
        popup.add(criarVerificarAgora());
        popup.add(criarSelecionarCidade());
        popup.add(criarConfigurarPapeisDeParede());
        popup.addSeparator();
        popup.add(criarSair(tray));

        iconeBandeja = new TrayIcon(TrayIconFactory.create(Theme.LIGHT), "NightOut", popup);
        iconeBandeja.setImageAutoSize(true);
        tray.add(iconeBandeja);
    }

    private static MenuItem criarStatusCidade() {
        statusItem = new MenuItem("Cidade: " + config.displayName());
        statusItem.setEnabled(false);
        return statusItem;
    }

    private static Menu criarMenuTema() {
        Menu menu = new Menu("Tema");
        menu.add(criarAutomatico());
        menu.addSeparator();
        menu.add(criarClaro());
        menu.add(criarEscuro());
        return menu;
    }

    private static MenuItem criarAutomatico() {
        return criarOpcaoDeTema("Automatico (pelo sol)", ThemeMode.AUTO);
    }

    private static MenuItem criarClaro() {
        return criarOpcaoDeTema("Claro (manual)", ThemeMode.LIGHT);
    }

    private static MenuItem criarEscuro() {
        return criarOpcaoDeTema("Escuro (manual)", ThemeMode.DARK);
    }

    private static MenuItem criarOpcaoDeTema(String label, ThemeMode modo) {
        CheckboxMenuItem item = new CheckboxMenuItem(label, config.themeMode == modo);
        item.addItemListener(e -> selecionarModoDeTema(modo));
        opcoesDeTema.put(modo, item);
        return item;
    }

    private static void selecionarModoDeTema(ThemeMode modo) {
        // Funciona como radio button: so a opcao escolhida fica marcada,
        // inclusive se o usuario clicar de novo na que ja estava marcada.
        opcoesDeTema.forEach((m, item) -> item.setState(m == modo));
        if (config.themeMode == modo) {
            return;
        }
        config.themeMode = modo;
        config.save();
        Logger.log("Modo do tema alterado para " + modo);
        scheduler.checkNow();
    }

    private static MenuItem criarVerificarAgora() {
        MenuItem item = new MenuItem(LABEL_VERIFICAR_AGORA);
        item.addActionListener(e -> scheduler.checkNow(
                resultado -> SwingUtilities.invokeLater(() -> mostrarResultadoDaVerificacao(resultado)),
                erro -> SwingUtilities.invokeLater(() -> mostrarErroDaVerificacao(erro))));
        return item;
    }

    private static void mostrarResultadoDaVerificacao(CheckResult resultado) {
        if (resultado == null) {
            notificar("Nenhuma cidade configurada.", TrayIcon.MessageType.WARNING);
        } else if (resultado.isManual()) {
            String tema = resultado.theme() == Theme.LIGHT ? "claro" : "escuro";
            notificar("Modo manual: tema " + tema + " fixo.\n"
                    + "Use Tema > Automatico para voltar a seguir o sol.", TrayIcon.MessageType.INFO);
        } else if (resultado.theme() == Theme.LIGHT) {
            notificar("Tema claro (dia).\nO sol se poe " + descreverHorario(resultado.nextSunset()) + ".",
                    TrayIcon.MessageType.INFO);
        } else {
            notificar("Tema escuro (noite).\nO sol nasce " + descreverHorario(resultado.nextSunrise()) + ".",
                    TrayIcon.MessageType.INFO);
        }
    }

    private static void mostrarErroDaVerificacao(Exception erro) {
        notificar("Nao foi possivel verificar o tema: " + erro.getMessage(), TrayIcon.MessageType.ERROR);
    }

    /** Ex.: "hoje as 18:37", "amanha as 05:41". */
    private static String descreverHorario(ZonedDateTime horario) {
        LocalDate dia = horario.toLocalDate();
        LocalDate hoje = LocalDate.now();
        String quando;
        if (dia.equals(hoje)) {
            quando = "hoje";
        } else if (dia.equals(hoje.plusDays(1))) {
            quando = "amanha";
        } else {
            quando = "em " + dia.format(DateTimeFormatter.ofPattern("dd/MM"));
        }
        return quando + " as " + horario.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static void notificar(String mensagem, TrayIcon.MessageType tipo) {
        if (iconeBandeja != null) {
            iconeBandeja.displayMessage("NightOut", mensagem, tipo);
        }
    }

    private static MenuItem criarSelecionarCidade() {
        MenuItem item = new MenuItem("Selecionar cidade...");
        item.addActionListener(e -> {
            CityResult picked = SettingsDialog.showAndSelect(config.displayName());
            if (picked != null) {
                applyCity(picked);
                statusItem.setLabel("Cidade: " + config.displayName());
                scheduler.checkNow();
            }
        });
        return item;
    }

    private static MenuItem criarConfigurarPapeisDeParede() {
        MenuItem item = new MenuItem("Configurar papeis de parede...");
        item.addActionListener(e -> {
            boolean saved = WallpaperSettingsDialog.showAndEdit(config);
            if (saved) {
                scheduler.invalidateWallpaperCache();
                scheduler.checkNow();
            }
        });
        return item;
    }

    private static MenuItem criarSair(SystemTray tray) {
        MenuItem item = new MenuItem("Sair");
        item.addActionListener(e -> {
            Logger.log("Encerrando NightOut.");
            scheduler.stop();
            tray.remove(iconeBandeja);
            System.exit(0);
        });
        return item;
    }

    private static void applyCity(CityResult city) {
        config.cityName = city.name();
        config.region = city.region();
        config.country = city.country();
        config.lat = city.lat();
        config.lon = city.lon();
        config.save();
        Logger.log("Cidade configurada: " + config.displayName() + " (" + config.lat + ", " + config.lon + ")");
    }

    private static void onThemeApplied(Theme theme) {
        SwingUtilities.invokeLater(() -> {
            if (iconeBandeja != null) {
                iconeBandeja.setImage(TrayIconFactory.create(theme));
                String label = theme == Theme.LIGHT ? "Tema claro (dia)" : "Tema escuro (noite)";
                if (config.themeMode != ThemeMode.AUTO) {
                    label += " - manual";
                }
                iconeBandeja.setToolTip("NightOut - " + config.displayName() + " - " + label);
            }
        });
    }
}
