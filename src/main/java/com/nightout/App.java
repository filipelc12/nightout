package com.nightout;

import javax.swing.*;
import java.awt.*;

import com.nightout.WindowsThemeManager.Theme;

/** Ponto de entrada: fica residente na bandeja do Windows trocando o tema pelo sol da cidade. */
public final class App {

    private static TrayIcon iconeBandeja;
    private static MenuItem statusItem;
    private static AppConfig config;
    private static ThemeScheduler scheduler;

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

    private static MenuItem criarVerificarAgora() {
        MenuItem item = new MenuItem(LABEL_VERIFICAR_AGORA);
        item.addActionListener(e -> scheduler.checkNow());
        return item;
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
                iconeBandeja.setToolTip("NightOut - " + config.displayName() + " - " + label);
            }
        });
    }
}
