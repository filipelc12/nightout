package com.nightout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Janela simples para o usuario buscar e escolher a cidade usada para calcular
 * o nascer/por do sol. Retorna a cidade escolhida (ou null se cancelado).
 */
public final class SettingsDialog {

    private SettingsDialog() {
    }

    public static CityResult showAndSelect(String currentCityLabel) {
        AtomicReference<CityResult> chosen = new AtomicReference<>();
        JDialog dialog = new JDialog((Frame) null, "NightOut - Selecionar cidade", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        JLabel currentLabel = new JLabel("Cidade atual: " + currentCityLabel);
        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Buscar");
        JPanel searchPanel = new JPanel(new BorderLayout(4, 4));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        topPanel.add(currentLabel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        DefaultListModel<CityResult> listModel = new DefaultListModel<>();
        JList<CityResult> resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setPreferredSize(new Dimension(420, 220));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JButton selectButton = new JButton("Selecionar");
        JButton cancelButton = new JButton("Cancelar");
        selectButton.setEnabled(false);
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(cancelButton);
        bottomPanel.add(selectButton);

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.add(statusLabel, BorderLayout.NORTH);
        southWrap.add(bottomPanel, BorderLayout.SOUTH);
        dialog.add(southWrap, BorderLayout.SOUTH);

        GeocodingService geocoding = new GeocodingService();

        Runnable doSearch = () -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) return;
            searchButton.setEnabled(false);
            statusLabel.setText("Buscando...");
            listModel.clear();
            new SwingWorker<List<CityResult>, Void>() {
                @Override
                protected List<CityResult> doInBackground() throws Exception {
                    return geocoding.search(query);
                }

                @Override
                protected void done() {
                    searchButton.setEnabled(true);
                    try {
                        List<CityResult> results = get();
                        for (CityResult r : results) listModel.addElement(r);
                        statusLabel.setText(results.isEmpty()
                                ? "Nenhuma cidade encontrada."
                                : results.size() + " cidade(s) encontrada(s).");
                    } catch (Exception e) {
                        statusLabel.setText("Erro na busca: " + e.getMessage());
                        Logger.log("Erro buscando cidade: " + e);
                    }
                }
            }.execute();
        };

        searchButton.addActionListener(e -> doSearch.run());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSearch.run();
            }
        });
        resultList.addListSelectionListener(e -> selectButton.setEnabled(resultList.getSelectedValue() != null));
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && resultList.getSelectedValue() != null) {
                    chosen.set(resultList.getSelectedValue());
                    dialog.dispose();
                }
            }
        });
        selectButton.addActionListener(e -> {
            chosen.set(resultList.getSelectedValue());
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return chosen.get();
    }
}
