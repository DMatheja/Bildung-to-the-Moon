package de.bildung.moon.views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Ein einfacher modal/Dialog mit einklappbaren Informationsabschnitten (Accordion).
 */
public class InfoDialog extends JDialog {

    public InfoDialog(Window owner) {
        super(owner, "Info & Fakten", ModalityType.APPLICATION_MODAL);
        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Add HTML-formatted sections
        content.add(createSection("Grundlagen der Raketentechnik", """
                <html><body style='font-family:sans-serif'>
                  <p><b>Raketen funktionieren nach dem Rückstoßprinzip</b> (3. Newtonsches Gesetz): Durch das Ausstoßen von Treibstoff mit hoher Geschwindigkeit entsteht eine Kraft in die entgegengesetzte Richtung, die die Rakete beschleunigt. Anders als Flugzeuge benötigen Raketen keine Luft, weshalb sie auch im Vakuum des Weltraums arbeiten können.</p>
                  <p>Um große Geschwindigkeiten zu erreichen, bestehen Raketen meist aus mehreren Stufen. Sobald eine Stufe ihren Treibstoff verbraucht hat, wird sie abgetrennt, um Gewicht zu sparen. Der Großteil der Raketenmasse besteht aus Treibstoff, da extrem hohe Geschwindigkeiten nötig sind, um die Erdanziehung zu überwinden oder andere Himmelskörper zu erreichen.</p>
                  <p>Weitere Informationen: <a href="https://www.nasa.gov/audience/forstudents/5-8/features/nasa-knows/what-is-a-rocket-58.html">NASA – What is a Rocket?</a></p>
                </body></html>
                """));

        content.add(createSection("Wichtige Raketenbauteile", """
                <html><body style='font-family:sans-serif'>
                  <p>Raketen bestehen aus vielen miteinander verbundenen Systemen, die zusammen den Start, Flug und die Navigation ins All möglich machen. Man kann die Bauteile nach ihrer Funktion in mehrere Hauptbereiche einteilen:</p>
                  <ul>
                    <li><b>Struktursystem:</b> Trägt das gesamte Fahrzeug, verbindet Bauteile und sorgt für Form und Stabilität der Rakete. Dazu gehören Hülle, Verstrebungen, Interstage-Adapter und aerodynamische Elemente.</li>
                    <li><b>Antriebssystem:</b> Besteht aus Triebwerken, Treibstoff- und Oxidatortanks, Pumpen und Leitungen. Es erzeugt den Schub, der die Rakete nach oben bewegt.</li>
                    <li><b>Leitsystem:</b> Umfasst Avionik, Sensoren und Steuerungselektronik. Diese Komponenten sorgen dafür, dass die Rakete korrekt gelenkt wird und auf der richtigen Bahn bleibt.</li>
                    <li><b>Nutzlast / Crew-Kapsel:</b> Das ist der Teil, der das eigentliche Missionsziel trägt – z. B. Satelliten, wissenschaftliche Instrumente oder eine Besatzung.</li>
                  </ul>
                  <p>Jedes dieser Systeme trägt auf seine Weise dazu bei, eine Rakete sicher und effizient zum Ziel zu bringen. In dieser Simulation werden nur einige dieser Bauteile in vereinfachter Form dargestellt.</p>
                  <p>Weitere Informationen: <a href="https://www1.grc.nasa.gov/beginners-guide-to-aeronautics/rocket-parts/">NASA-Erklärung zu Raketenbauteilen</a></p>
                </body></html>
                """));

        content.add(createSection("Der Mond", """
                <html><body style='font-family:sans-serif'>
                  <p>Der Mond ist der einzige natürliche Satellit der Erde und befindet sich im Durchschnitt etwa 384.400 km von ihr entfernt. Er hat etwa 1/6 der Erdgravitation und keine nennenswerte Atmosphäre. Seine Oberfläche ist geprägt von Einschlagkratern.</p>
                  <p>Der Mond beeinflusst Gezeiten und stabilisiert die Achsneigung der Erde. Seit den Apollo-Missionen gilt er als wichtiger Testort für Raumfahrttechnologien.</p>
                  <p>Weitere Informationen: <a href="https://solarsystem.nasa.gov/moons/earths-moon/overview/">NASA – Earth's Moon</a></p>
                </body></html>
                """));

        content.add(createSection("Tipps fürs Spiel", """
                <html><body style='font-family:sans-serif'>
                  <ul>
                    <li>Achte auf Masse vs. Schub (TWR) und auf Isp für Effizienz.</li>
                    <li>Stage-Aufteilung: benutze abwerfbare Tanks/Triebwerke, um Masse zu sparen.</li>
                    <li>Test verschiedene Kombinationen und beobachte G-Forces.</li>
                  </ul>
                </body></html>
                """));

        // Glue to keep content left aligned
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(800, 520));

        // Close Button
        JButton close = new JButton("Schließen");
        close.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(close);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);

        setContentPane(wrapper);
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }

    private JComponent createSection(String title, String htmlContent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton header = new JButton(title + "  [+]");
        header.setFocusPainted(false);
        header.setContentAreaFilled(false);
        header.setBorderPainted(false);
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));

        JEditorPane body = new JEditorPane("text/html", htmlContent);
        body.setEditable(false);
        body.setOpaque(false);
        body.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        body.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = e.getURL().toString();
                    int r = JOptionPane.showConfirmDialog(panel, "Link im Browser öffnen?\n" + url, "Link öffnen", JOptionPane.YES_NO_OPTION);
                    if (r != JOptionPane.YES_OPTION) return;
                    // Öffne im Hintergrund, damit EDT nicht blockiert
                    new Thread(() -> {
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(new URI(url));
                            } else {
                                SwingUtilities.invokeLater(() -> {
                                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                                    JOptionPane.showMessageDialog(panel, "Desktop nicht unterstützt. Link in die Zwischenablage kopiert: " + url);
                                });
                            }
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                                JOptionPane.showMessageDialog(panel, "Konnte Link nicht öffnen. Link wurde kopiert:\n" + ex.getMessage());
                            });
                        }
                    }).start();
                }
            }
        });

        // Start hidden; we will expand the JEditorPane when opened so the outer JScrollPane handles scrolling
        body.setVisible(false);

        header.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean open = !body.isVisible();
                body.setVisible(open);

                if (open) {
                    // Compute preferred height based on a fixed width so the body expands to full content
                    int contentWidth = 680;
                    body.setSize(contentWidth, Short.MAX_VALUE);
                    Dimension pref = body.getPreferredSize();
                    body.setPreferredSize(new Dimension(contentWidth, pref.height));
                } else {
                    body.setPreferredSize(null);
                }

                header.setText(title + (open ? "  [-]" : "  [+]"));
                panel.revalidate();
                Window w = SwingUtilities.getWindowAncestor(panel);
                if (w instanceof Window) {
                    ((Window) w).pack();
                }
            }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }
}