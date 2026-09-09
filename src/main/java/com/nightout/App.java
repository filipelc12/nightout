package com.nightout;

import javax.swing.*;
import java.awt.*;

import com.nightout.WindowsThemeManager.Theme;

/** Ponto de entrada: fica residente na bandeja do Windows trocando o tema pelo sol da cidade. */
public final class App {

    private static TrayIcon trayIcon;
    private static MenuItem statusItem;
    private static AppConfig config;
    private static ThemeScheduler scheduler;

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
            setupTrayIcon();
        } catch (AWTException e) {
            Logger.log("Falha ao criar icone na bandeja: " + e);
            return;
        }

        scheduler.start();
    }

    private static void setupTrayIcon() throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();

        PopupMenu popup = new PopupMenu();

        statusItem = new MenuItem("Cidade: " + config.displayName());
        statusItem.setEnabled(false);
        popup.add(statusItem);
        popup.addSeparator();

        MenuItem checkNowItem = new MenuItem("Verificar agora");
        checkNowItem.addActionListener(e -> scheduler.checkNow());
        popup.add(checkNowItem);

        MenuItem changeCityItem = new MenuItem("Selecionar cidade...");
        changeCityItem.addActionListener(e -> {
            CityResult picked = SettingsDialog.showAndSelect(config.displayName());
            if (picked != null) {
                applyCity(picked);
                statusItem.setLabel("Cidade: " + config.displayName());
                scheduler.checkNow();
            }
        });
        popup.add(changeCityItem);

        popup.addSeparator();
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(e -> {
            Logger.log("Encerrando NightOut.");
            scheduler.stop();
            tray.remove(trayIcon);
            System.exit(0);
        });
        popup.add(exitItem);

        trayIcon = new TrayIcon(TrayIconFactory.create(Theme.LIGHT), "NightOut", popup);
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);
    }

    private static void applyCity(CityResult city) {
        config.cityName = city.name();
        config.admin1 = city.admin1();
        config.country = city.country();
        config.lat = city.lat();
        config.lon = city.lon();
        config.save();
        Logger.log("Cidade configurada: " + config.displayName() + " (" + config.lat + ", " + config.lon + ")");
    }

    private static void onThemeApplied(Theme theme) {
        SwingUtilities.invokeLater(() -> {
            if (trayIcon != null) {
                trayIcon.setImage(TrayIconFactory.create(theme));
                String label = theme == Theme.LIGHT ? "Tema claro (dia)" : "Tema escuro (noite)";
                trayIcon.setToolTip("NightOut - " + config.displayName() + " - " + label);
            }
        });
    }
}
