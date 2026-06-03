package windclan.scrobbler;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import nz.ac.waikato.cms.adams.simpledirectorychooser.SimpleDirectoryChooser;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.util.Scanner;

public class Main {
    public static String API = "https://ws.audioscrobbler.com/2.0";
    public static String API_KEY = "c8165a5013dab1a9628d7a4781adaf5e";
    public static String SHARED_SECRET = "2f4bab51ac3615a3e3ab61165eed323b";

    public static String SESSION_PATH = System.getProperty("user.home")+"/playbacklogscrobbler.conf";

    public static String SESSION_KEY = "";

    public static String DEVICE_ROOT = "";

    public static ArrayList<Scrobble> parseScrobbles(String path, String deviceRoot) {
        ArrayList<Scrobble> scrobbles = LogParser.parseLog(path,deviceRoot);
        if (!scrobbles.isEmpty()) {
            Scrobble lastScrobble = scrobbles.get(0);
            for (int i = 1; i < scrobbles.size(); i++) {
                Scrobble thisScrobble = scrobbles.get(i);
                if ((thisScrobble.getDuration()*0.9) >= thisScrobble.getElapsed() && (thisScrobble.getTrack().equals(lastScrobble.getTrack()) && thisScrobble.getArtist().equals(lastScrobble.getArtist()) && thisScrobble.getDuration() == lastScrobble.getDuration())) {
                    scrobbles.remove(i);
                    i--;
                }
                lastScrobble = thisScrobble;
            }
        }
        return scrobbles;
    }
    public static void sendScrobbles(ArrayList<Scrobble> scrobbleList, Session session) {
        List<ScrobbleData> scrobbleDatList = new ArrayList<>();
        for (int i = 0; i < scrobbleList.size(); i++) {
            if (i%50 == 0 && i != 0) {
                Track.scrobble(scrobbleDatList,session);
                scrobbleDatList = new ArrayList<>();
            }
            Scrobble s = scrobbleList.get(i);
            ScrobbleData sd = new ScrobbleData(s.getArtist(),s.getTrack(),s.getTimestamp());
            sd.setDuration(s.getDuration());
            sd.setChosenByUser(true);
            scrobbleDatList.add(sd);
        }
        Track.scrobble(scrobbleDatList,session);
    }

    public static void makeLoginScreen() {
        JFrame frame = new JFrame();
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);
        frame.setSize(220,115);
        frame.getContentPane().setPreferredSize(new Dimension(220, 115));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);

        JLabel label = new JLabel("last.fm login");
        label.requestFocus();
        label.setBounds(5,0,200,25);
        frame.add(label);


        JLabel label1 = new JLabel("Username");
        label1.requestFocus();
        label1.setBounds(5,25,100,25);
        frame.add(label1);

        JTextField username = new JTextField();
        username.setBounds(110,25,105,25);
        frame.add(username);

        JLabel label2 = new JLabel("Password");
        label2.requestFocus();
        label2.setBounds(5,55,100,25);
        frame.add(label2);

        JPasswordField password = new JPasswordField();
        password.setEchoChar('*');
        password.setBounds(110,55,105,25);
        frame.add(password);

        JButton done = new JButton("Done");
        done.setBounds(5,85,100,25);
        done.addActionListener(
                e -> {
                    Session s = Authenticator.getMobileSession(username.getText(),String.valueOf(password.getPassword()),API_KEY,SHARED_SECRET);
                    if (s.getKey() != null) {
                        SESSION_KEY = s.getKey();
                        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        frame.setVisible(false);
                        frame.dispose();
                    }
                });
        frame.add(done);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        Caller.getInstance().setApiRootUrl(API);
        File conf = new File(SESSION_PATH);
        if (conf.canRead() && conf.exists()) {
            try {
                Scanner s = new Scanner(conf);
                SESSION_KEY = s.nextLine();
                s.close();
            } catch (Exception ignored) {}
        }
        if (SESSION_KEY.isEmpty()) {
            makeLoginScreen();
            try {
                while (SESSION_KEY.isEmpty()) {
                    Thread.sleep(10);
                }
                FileWriter fileWriter = new FileWriter(conf);
                fileWriter.write(SESSION_KEY);
                fileWriter.close();
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        SimpleDirectoryChooser chooser = new SimpleDirectoryChooser();
        chooser.setDialogTitle("Select MP3 player drive");
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            DEVICE_ROOT = chooser.getSelectedFile().getAbsolutePath()+"/";
            String logPath = DEVICE_ROOT+".rockbox/playback.log";
            File log = new File(logPath);
            if (log.exists()) {
                ArrayList<Scrobble> scrobbles = parseScrobbles(logPath,DEVICE_ROOT);
                if (scrobbles.size() == 0) {
                    JOptionPane.showMessageDialog(null,"Nothing to scrobble!");
                } else {
                    JOptionPane.showMessageDialog(null,"Press OK to submit "+scrobbles.size()+" scrobbles");
                    Session s = Session.createSession(API_KEY,SHARED_SECRET,SESSION_KEY);
                    sendScrobbles(scrobbles,s);
                    String oldLogPath = DEVICE_ROOT+".rockbox/playback_old.log";
                    File oldLog = new File(oldLogPath);
                    try {
                        Files.copy(log.toPath(),oldLog.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch(Exception ignored){}
                    log.delete();
                }
            } else {
                JOptionPane.showMessageDialog(null,"Failed to locate "+logPath);
            }
        } else {
            JOptionPane.showMessageDialog(null,"Cancelled Scrobbling!");
        }

    }
}