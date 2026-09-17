package com.nightout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Janela simples para o usuario escolher os arquivos de imagem usados como
 * papel de parede no tema claro (dia) e no tema escuro (noite). Atualiza o
 * AppConfig recebido e o salva se o usuario confirmar.
 */
public final class WallpaperSettingsDialog {

    /** Largura (em colunas) dos campos de caminho, soh pra caber um path completo sem ficar minusculo. */
    private static final int PATH_FIELD_COLUMNS = 30;

    private WallpaperSettingsDialog() {
    }

    /** Mostra o dialogo; retorna true se o usuario salvou as alteracoes. */
    public static boolean showAndEdit(AppConfig config) {
        AtomicBoolean saved = new AtomicBoolean(false);
        JDialog dialog = new JDialog((Frame) null, "NightOut - Papeis de parede", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dayField = new JTextField(
                config.wallpaperDayPath == null ? "" : config.wallpaperDayPath, PATH_FIELD_COLUMNS);
        JTextField nightField = new JTextField(
                config.wallpaperNightPath == null ? "" : config.wallpaperNightPath, PATH_FIELD_COLUMNS);
        JButton dayBrowse = new JButton("Procurar...");
        JButton nightBrowse = new JButton("Procurar...");

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Wallpaper do dia (tema claro):"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1;
        form.add(dayField, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0;
        form.add(dayBrowse, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Wallpaper da noite (tema escuro):"), gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1;
        form.add(nightField, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0;
        form.add(nightBrowse, gbc);

        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "Imagens (*.jpg, *.jpeg, *.png, *.bmp)", "jpg", "jpeg", "png", "bmp");

        dayBrowse.addActionListener(e -> pickFile(dialog, imageFilter, dayField));
        nightBrowse.addActionListener(e -> pickFile(dialog, imageFilter, nightField));

        JButton saveButton = new JButton("Salvar");
        JButton cancelButton = new JButton("Cancelar");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(cancelButton);
        bottomPanel.add(saveButton);

        saveButton.addActionListener(e -> {
            config.wallpaperDayPath = dayField.getText().trim();
            config.wallpaperNightPath = nightField.getText().trim();
            config.save();
            saved.set(true);
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return saved.get();
    }

    private static void pickFile(JDialog parent, FileNameExtensionFilter filter, JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        String current = target.getText().trim();
        if (!current.isEmpty()) {
            File currentFile = new File(current);
            if (currentFile.getParentFile() != null) {
                chooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}
