
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class MyFrame extends JFrame {
    private JPanel contentPane;

    private int WINDOW_WIDTH = Global.WINDOW_WIDTH;
    private int WINDOW_HEIGHT = Global.WINDOW_HEIGHT;

    private int FPS = Global.FPS;

    private String VIDEO_FILE = "C:\\Users\\pisha\\IdeaProjects\\FaceToBpm\\src\\face_video.mp4";

//    private VideoCap videoCap = new VideoCap(VIDEO_FILE);
    private VideoCap videoCap = new VideoCap();

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MyFrame frame = new MyFrame();
                    frame.videoCap.setFaceTrackingEnable(true);
                    frame.videoCap.setFORE_HEAD_DETECTION_PERCENTAGE(80);
                    frame.videoCap.setFACE_DETECTION_PERCENTAGE(100);
                    frame.videoCap.setHeartRateCalculationEnabled(true);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public MyFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, WINDOW_WIDTH, WINDOW_HEIGHT);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // When user is pressing close button
                System.out.println("Close Pressed");
                if (videoCap != null) {
                    videoCap.stopCap();
                }
                super.windowClosing(e);
            }
        });

        new ThreadForFPSRender().start();
    }


    public void paint(Graphics g) {
        g = contentPane.getGraphics();
        g.drawImage(videoCap.getOneFrame(), 0, 0, this);
    }

    class ThreadForFPSRender extends Thread {
        @Override
        public void run() {
            for (; ; ) {
                repaint();
                try {
                    Thread.sleep(1000/FPS);  // FPS is in milliseconds
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

