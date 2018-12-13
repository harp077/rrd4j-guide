package pkg4j.rrd.snmp;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class RrdSnmp {

    static long step_d = 60L, // sek
            step_w = 420L,
            step_m = 1680L,
            heartbeat = 60L;
    static String rrdPathDB = "./rrd/my.rrd";
    static String DS = "loss-ds";
    static double loss;
    static ConsolFun CF = ConsolFun.AVERAGE;
    static Sample sample;
    static long END;
    static long numfor = 100;
    static RrdDb rrdDb;
    static RrdSnmp rrdSnmp;

    static {    // analog @PostConstruct
        File rrddir = new File("./rrd");
        rrddir.mkdir();
        File imgdir = new File("./img");
        imgdir.mkdir(); 
        /*if (!FileUtils.getFile("./rrd").exists()) try {
            FileUtils.forceMkdir(new File("./rrd"));
        } catch (IOException ex) {   }*/        
        try {
            rrdDb = new RrdDb(dbPrepare(rrdPathDB));
            //rrdDb = new RrdDb("./rrd/my.rrd"); // open RW exists
        } catch (IOException ex) {
            Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void rrdImgDraw(String fname, long step, long alltime, String title) {
        RrdGraphDef gD = new RrdGraphDef();
        gD.setWidth(720);
        gD.setHeight(150);
        gD.setFilename(fname);
        gD.setTitle(title);
        gD.setVerticalLabel("percent, %");
        //gD.comment("Packet Loss in %");
        gD.datasource("loss-average-d", rrdPathDB, DS, CF);
        gD.setStep(step);
        //gD.setStartTime(-86400L);
        //gD.setEndTime(System.currentTimeMillis() / 1000);        
        //gD.setTimeSpan(-alltime, System.currentTimeMillis() / 1000+numfor*60L);
        gD.setTimeSpan(-alltime + numfor * 60L, Util.getTimestamp() + numfor * 60L);
        //gD.setColor(RrdGraphConstants.COLOR_GRID, Color.GREEN);
        //gDef_d.line("loss-average", Color.MAGENTA, "Packet Loss in %");
        gD.area("loss-average-d", Color.MAGENTA, "Packet Loss in %");
        //gDef_d.hrule(2568, Color.GREEN, "hrule");
        gD.setImageFormat("png");
        try {
            RrdGraph graph = new RrdGraph(gD);
        } catch (IOException ex) {
        }
    }

    public static RrdDef dbPrepare(String pathDB) {
        RrdDef rrdDef = new RrdDef(pathDB, step_d);
        rrdDef.addDatasource(DS, DsType.GAUGE, heartbeat, 0.0, 100.0);
        rrdDef.addArchive(CF, 0.5, 1, 1440); // day - 1 min
        rrdDef.addArchive(CF, 0.5, 7, 1440); // week - 7 min
        rrdDef.addArchive(CF, 0.5,28, 1440); // month=28day - 28 min
        return rrdDef;
    }
    
    public static void main(String[] args) {
        //try (RrdDb rrdDb = new RrdDb(dbPrepare())) {
        try {
            END = Util.getTimestamp();//System.currentTimeMillis();//Util.getTimestamp();
            for (int i = 0; i < numfor; i++) {
                sample = rrdDb.createSample();
                loss = 1 + i;
                //sample.setTime(END-30*60000+i*60000);
                //sample.setValue(DS, loss);
                sample.setAndUpdate(END + i * 60L + ":" + loss);
                //sample.update();
                System.out.println("@@@ = " + i + " step, DS-names = " + sample.getDsNames()[0]);
            }
            //sample.update();
        } catch (IOException ex) {
            Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
        //////////////////                      DAY
        rrdImgDraw("./img/loss-d.png", step_d, 1 * 86_400L, "Daily packet Loss in %");
        ///////////////////////                 WEEK
        rrdImgDraw("./img/loss-w.png", step_w, 7 * 86_400L, "Weekly packet Loss in %");
        ///////////////////////                 MONTH
        rrdImgDraw("./img/loss-m.png", step_m,28 * 86_400L, "Monthly packet Loss in %");
        ////////////////
        try {
            rrdDb.close();
        } catch (IOException ex) {
            Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
        ///////////////////////////////////////////////
        JLabel zag = new JLabel("RRD4j demo test guide");
        JLabel lbl01 = new JLabel();
        lbl01.setIcon(new ImageIcon("./img/loss-d.png"));
        JLabel lbl02 = new JLabel();
        lbl02.setIcon(new ImageIcon("./img/loss-w.png")); 
        JLabel lbl03 = new JLabel();
        lbl03.setIcon(new ImageIcon("./img/loss-m.png")); 
        JButton jb=new JButton("Run RRD DB inspector");
        /*jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    Runtime.getRuntime().exec("java -jar ./lib/rrd4j-3.2-inspector.jar " + rrdPathDB);
                } catch (Exception ex) {
                    Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }}); */        
        jb.addActionListener((ActionEvent evt) -> {
            try {
                /*ProcessBuilder pb = new ProcessBuilder("java -jar rrd4j-3.2-inspector.jar",rrdPathDB);
                pb.directory(new File("./lib"));
                pb.start();*/
                Runtime.getRuntime().exec("java -jar ./lib/rrd4j-inspector-3.2.jar " + rrdPathDB);
            } catch (Exception ex) {
                Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
            }
        });  
        //File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
        //System.out.println(jarDir.getAbsolutePath());
        Object[] ob = {zag, lbl01, lbl02, lbl03, jb};
        JOptionPane.showMessageDialog(null, ob, "RRD4j demo test guide", JOptionPane.CLOSED_OPTION, new ImageIcon("./lib/logo.png"));
    }

}
