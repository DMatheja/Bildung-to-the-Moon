package de.bildung.moon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Das Hauptfenster (JFrame), das den GameCanvas (JPanel) enthält.
 */
public class GameWindow extends JFrame {
    private final JFrame parent;
    private final GameCanvas gameCanvas;

    GameWindow(JFrame parent) {
        super("Bildung to the Moon - Rocket Builder");
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameCanvas = new GameCanvas();
        add(gameCanvas, BorderLayout.CENTER);
        pack();
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onReturn();
            }
        });
        setVisible(true);
        showTutorialDialog();
    }


    private void showTutorialDialog(){
        //Dialogfenster erstellen
        JDialog tutorialDialog = new JDialog(this,"Tutorial - Bildung to the Moon - Rocket Builder");
        tutorialDialog.setModal(true); //Tutorial muss erst geschlossen werden bevor weiter spielen

        //Inhalt festlegen
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel text = new JLabel(
                "<html><body style='width: 600px;'>" +
                        "<h2>Willkommen bei: Bildung to the Moon – dem Raketenbauspiel</h2>" +

                        "<p>In diesem Spiel kannst du mithilfe der Raketenbauteile aus dem Shop " +
                        "deine eigene Rakete zusammenbauen und unter Berücksichtigung von z.&nbsp;B. " +
                        "der Schwerkraft oder der Anziehung des Mondes realitätsnah simulieren, " +
                        "wie gut die Rakete fliegt. Wie weit schaffst du es?</p>" +

                        "<ul>" +
                        "<li>Baue deine Rakete mit den verfügbaren Modulen aus dem Shop.</li>" +
                        "<li>Wähle mit der linken Maustaste ein Teil aus und platziere es im Raster an einem erlaubten Platz.</li>" +
                        "<li>Nicht erlaubte Positionen werden mit einem Kreuz gekennzeichnet.</li>" +
                        "<li>Mit der rechten Maustaste kannst du bereits platzierte Teile wieder entfernen.</li>" +
                        "<li>Tipp: Achte beim Zusammenbau auf Gewicht, Kosten und Effizienz, um möglichst weit zu kommen.</li>" +
                        "<li>Starte den Flug mit „To Launchpad“ und versuche, den Mond zu erreichen!</li>" +
                        "</ul>" +
                        "</body></html>"
        );

        //close Button
        JButton closeButton = new JButton("Alles klar!");
        closeButton.addActionListener(e -> tutorialDialog.dispose());

        //Dialogstyling
        content.add(text, BorderLayout.CENTER);
        content.add(closeButton, BorderLayout.SOUTH);

        tutorialDialog.setContentPane(content);
        tutorialDialog.pack();
        tutorialDialog.setLocationRelativeTo(this);
        tutorialDialog.setResizable(false);

        // sichtbar machen wenn alles gebaut
        tutorialDialog.setVisible(true);
    }





    private void onReturn() {
        gameCanvas.stopGameLoop();
        dispose();
        if (parent != null) {
            parent.setVisible(true);
        }
    }
}
