/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.BASE64ToolKit;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Shows reports, with an Okay JButton
 */
public class MiniReportDisplay extends JDialog implements ActionListener, IPreferenceChangeListener {
    private JButton butOkay;
    private JPanel panelMain;
    private JTabbedPane tabs;  
    private Client currentClient;

    private static final String MRD_TITLE = Messages.getString("MiniReportDisplay.title");
    private static final String MRD_ROUND = Messages.getString("MiniReportDisplay.Round");
    private static final String MRD_PHASE = Messages.getString("MiniReportDisplay.Phase");
    private static final String MRD_OKAY= Messages.getString("Okay");

    public MiniReportDisplay(JFrame parent, Client client) {
        super(parent, MRD_TITLE, false);

        if (client == null) {
            return;
        }

        currentClient = client;
        currentClient.getGame().addGameListener(gameListener);

        butOkay = new JButton(MRD_OKAY);
        butOkay.addActionListener(this);

        panelMain = new JPanel(new BorderLayout());

        panelMain.add(BorderLayout.SOUTH, butOkay);
        
        setupReportTabs();
                
        setSize(GUIPreferences.getInstance().getMiniReportSizeWidth(),
                GUIPreferences.getInstance().getMiniReportSizeHeight());
        doLayout();
        setLocation(GUIPreferences.getInstance().getMiniReportPosX(),
                GUIPreferences.getInstance().getMiniReportPosY());

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                        ActionEvent.ACTION_PERFORMED, butOkay.getText()));
            }
        });

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);

        panelMain.add(tabs);
        add(panelMain);

        butOkay.requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            savePrefHide();
        }
    }


    private void setupReportTabs() {
        tabs = new JTabbedPane();

        addReportPages();
        
        getContentPane().add(BorderLayout.CENTER, tabs);
    }

    private void savePrefHide() {
        GUIPreferences.getInstance().setMiniReportSizeWidth(getSize().width);
        GUIPreferences.getInstance().setMiniReportSizeHeight(getSize().height);
        GUIPreferences.getInstance().setMiniReportPosX(getLocation().x);
        GUIPreferences.getInstance().setMiniReportPosY(getLocation().y);
        setVisible(false);
    }

    public void addReportPages() {
        int numRounds = currentClient.getGame().getRoundCount();
        tabs.removeAll();

        for (int round = 1; round <= numRounds; round++) {
            String text = currentClient.receiveReport(currentClient.getGame().getReports(round));
            JTextPane ta = new JTextPane();
            ReportDisplay.setupStylesheet(ta);
            BASE64ToolKit toolKit = new BASE64ToolKit();
            ta.setEditorKit(toolKit);
            ta.setText("<pre>" + text + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);
            tabs.add(MRD_ROUND + " " + round, new JScrollPane(ta));
        }

        // add the new current phase tab
        JTextPane ta = new JTextPane();
        ReportDisplay.setupStylesheet(ta);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        ta.setEditorKit(toolKit);
        ta.setText("<pre>" + currentClient.phaseReport + "</pre>");
        ta.setEditable(false);
        ta.setOpaque(false);

        JScrollPane sp = new JScrollPane(ta);
        tabs.add(MRD_PHASE, sp);

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            switch (e.getOldPhase()) {
                case VICTORY:
                    savePrefHide();
                    break;
                default:
                    if (!e.getNewPhase().equals((e.getOldPhase()))) {
                        addReportPages();
                    }
            }
        }
    };

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);

        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component cp = tabs.getComponentAt(i);
            if (cp instanceof JScrollPane) {
                Component pane = ((JScrollPane) cp).getViewport().getView();
                if (pane instanceof JTextPane) {
                    JTextPane tp = (JTextPane) pane;
                    ReportDisplay.setupStylesheet(tp);
                    tp.setText(tp.getText());
                }
            }
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}
