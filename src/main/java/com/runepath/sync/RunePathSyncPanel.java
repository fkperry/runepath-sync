package com.runepath.sync;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

final class RunePathSyncPanel extends PluginPanel
{
    private final JLabel statusLabel = new JLabel("Disconnected", SwingConstants.CENTER);
    private final JLabel pairingLabel = new JLabel("No pairing code", SwingConstants.CENTER);

    RunePathSyncPanel(Runnable connect, Runnable syncNow, Runnable forget)
    {
        setLayout(new BorderLayout(0, 12));
        JPanel content = new JPanel(new GridLayout(0, 1, 0, 9));
        content.add(new JLabel("RunePath Sync", SwingConstants.CENTER));
        content.add(statusLabel);
        content.add(pairingLabel);

        JButton connectButton = new JButton("Connect RunePath");
        connectButton.addActionListener(e -> connect.run());
        content.add(connectButton);

        JButton websiteButton = new JButton("Open RunePath");
        websiteButton.setToolTipText("Open your RunePath account in your default browser");
        websiteButton.addActionListener(e -> LinkBrowser.browse("https://runepath.floot.app/#account"));
        content.add(websiteButton);

        JButton syncButton = new JButton("Sync Now");
        syncButton.addActionListener(e -> syncNow.run());
        content.add(syncButton);

        JButton forgetButton = new JButton("Forget Local Token");
        forgetButton.addActionListener(e -> forget.run());
        content.add(forgetButton);

        add(content, BorderLayout.NORTH);
    }

    void status(String value)
    {
        SwingUtilities.invokeLater(() -> statusLabel.setText(value));
    }

    void code(String value)
    {
        SwingUtilities.invokeLater(() -> pairingLabel.setText(value));
    }
}
