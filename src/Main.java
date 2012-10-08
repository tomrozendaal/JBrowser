import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;

import static javafx.concurrent.Worker.State.FAILED;

public class Main implements Runnable {
    private JFXPanel jfxPanel;
    private WebEngine engine;

    private JFrame frame = new JFrame();
    private JPanel panel = new JPanel(new BorderLayout());
    private JLabel lblStatus = new JLabel();
    
    private JButton reload = new JButton(new ImageIcon("src/img/refresh.png"));
    private JButton prev = new JButton(new ImageIcon("src/img/prev.png"));
    private JButton next = new JButton(new ImageIcon("src/img/next.png"));
    private JTextField txtURL = new JTextField();
    private JProgressBar progressBar = new JProgressBar();

    private void initComponents() {
        jfxPanel = new JFXPanel();

        createScene();

        ActionListener al = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                loadURL(txtURL.getText());
                txtURL.setScrollOffset(0);
            }
        };
        
        ActionListener alReload = new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		reload();
        	}
        };
        
        ActionListener alPrev = new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		goBack();
        	}
        };
        
        ActionListener alNext = new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		goNext();
        	}
        };
        
        txtURL.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                SwingUtilities.invokeLater( new Runnable() {
                	public void run() {
                    	txtURL.selectAll();              
                    }
                });
            }
        });

        txtURL.addActionListener(al);
        reload.addActionListener(alReload);
        prev.addActionListener(alPrev);
        next.addActionListener(alNext);

        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);
        
        JPanel navigation = new JPanel();
        navigation.add(prev, BorderLayout.WEST);
        navigation.add(next, BorderLayout.CENTER);
        navigation.add(reload, BorderLayout.EAST);

        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topBar.add(navigation, BorderLayout.WEST);
        topBar.add(txtURL, BorderLayout.CENTER);


        JPanel statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(jfxPanel, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        
        frame.getContentPane().add(panel);
    }
    
    protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }

    private void createScene() {

        Platform.runLater(new Runnable() {
            @Override public void run() {

                WebView view = new WebView();
                engine = view.getEngine();

                engine.titleProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                frame.setTitle(newValue);
                                txtURL.setScrollOffset(0);
                            }
                        });
                    }
                });

                engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
                    @Override public void handle(final WebEvent<String> event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                lblStatus.setText(event.getData());
                            }
                        });
                    }
                });

                engine.locationProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                txtURL.setText(newValue);
                            }
                        });
                    }
                });

                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });

                engine.getLoadWorker()
                        .exceptionProperty()
                        .addListener(new ChangeListener<Throwable>() {
                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override public void run() {
                                            JOptionPane.showMessageDialog(
                                                panel,
                                                (value != null) ?
                                                engine.getLocation() + "\n" + value.getMessage() :
                                                engine.getLocation() + "\nUnexpected error.",
                                                "Loading error...",
                                                JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });

                jfxPanel.setScene(new Scene(view));
            }
        });
    }

    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                String tmp = toURL(url);

                if (tmp == null) {
                    tmp = toURL("http://" + url);
                }

                engine.load(tmp);
            }
        });
    }
    
    public void reload(){
    	Platform.runLater(new Runnable() {
    		public void run(){
    			engine.reload();
    		}
    	});
    }
    
    public void goBack(){
    	Platform.runLater(new Runnable(){
    		public void run(){
    			engine.executeScript("history.back()");
    		}
    	});
    }
    
    public void goNext(){
    	Platform.runLater(new Runnable(){
    		public void run(){
    			engine.executeScript("history.go(+1)");
    		}
    	});
    }

    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException exception) {
                return null;
        }
    }

    @Override public void run() {
        frame.setPreferredSize(new Dimension(1024, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();

        loadURL("http://www.google.com");

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Main());
    }
}