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
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.data.Variable;

public class RrdSnmp {

    static int KOF=2;
    static long step_d =     60L*KOF, // sek
                step_w =   7*60L*KOF,
                step_m = 4*7*60L*KOF,
                heartbeat  = 60L*2*KOF;
    static String rrdPathDB = "./rrd/my.rrd";
    static String DSinp = "ds-inp";
    static String DSout = "ds-out";
    static double input;
    static double output;
    static ConsolFun CF_AVE = ConsolFun.AVERAGE;
    static ConsolFun CF_MAX = ConsolFun.MAX;
    static ConsolFun CF_MIN = ConsolFun.MIN;
    static ConsolFun CF_CUR = ConsolFun.LAST;
    static Sample sample;
    static long END;
    static long numfor = Math.round(1000/KOF);
    static RrdDb rrdDb;
    //static RrdSnmp rrdSnmp;
    static RrdGraphDef gd;
    static double MAX_BANDWIDTH=9_999_999_999.0; // 10 GBit/s
    // 1     Mbit/s =       999_999
    // 10    Mbit/s =     9_999_999
    // 100   Mbit/s =    99_999_999 
    // 1000  Mbit/s =   999_999_999     
    // 10000 Mbit/s = 9_999_999_999     

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
        gd = new RrdGraphDef();
        gd.setWidth(720);
        gd.setHeight(150);
        gd.setFilename(fname);
        gd.setTitle(title);
        gd.setVerticalLabel("bit/s");
        //gd.comment("comment");
        gd.datasource(DSinp+"1", rrdPathDB, DSinp, CF_AVE);
        gd.datasource(DSout+"1", rrdPathDB, DSout, CF_AVE);
        gd.datasource("max-inp", DSinp+"1", new Variable.MAX());
        gd.datasource("cur-inp", DSinp+"1", new Variable.LAST());
        gd.datasource("min-inp", DSinp+"1", new Variable.MIN());        
        gd.datasource("ave-inp", DSinp+"1", new Variable.AVERAGE());
        gd.datasource("max-out", DSout+"1", new Variable.MAX());
        gd.datasource("cur-out", DSout+"1", new Variable.LAST());
        gd.datasource("min-out", DSout+"1", new Variable.MIN());        
        gd.datasource("ave-out", DSout+"1", new Variable.AVERAGE());          
        gd.setStep(step);
        //gD.setStartTime(-86400L);
        //gD.setEndTime(System.currentTimeMillis() / 1000);        
        //gD.setTimeSpan(-alltime, System.currentTimeMillis() / 1000+numfor*60L);
        gd.setTimeSpan(Util.getTimestamp() - alltime + numfor * 60L * KOF, Util.getTimestamp() + numfor * 60L * KOF);
        //gd.setColor(RrdGraphConstants.COLOR_GRID, Color.LIGHT_GRAY);
        //gd.comment("\\r"); // right align
        gd.area(DSinp+"1", Color.GREEN, " INPUT - bit/s, "); 
        gd.gprint("min-inp", "MINIMUM = %.3f%s, "); 
        gd.gprint("ave-inp", "AVERAGE = %.3f%s, "); 
        gd.gprint("max-inp", "MAXIMUM = %.3f%s, ");        
        gd.gprint("cur-inp", "CURRENT = %.3f%s, ");        
        gd.comment("\\l"); // left align       
        gd.line(DSout+"1", Color.BLUE, "OUTPUT - bit/s, "); 
        gd.gprint("min-out", "MINIMUM = %.3f%s, "); 
        gd.gprint("ave-out", "AVERAGE = %.3f%s, ");
        gd.gprint("max-out", "MAXIMUM = %.3f%s, ");        
        gd.gprint("cur-out", "CURRENT = %.3f%s, ");        
        gd.comment("\\l");  // left align
            //gd.hrule(20.0, Color.GREEN, "hrule");
            //gd.hspan(5.0, 9.0, Color.LIGHT_GRAY, "hspan");
            //gd.setAltAutoscale(true);
            //gd.setAltAutoscaleMax(true);
            //gd.setAltAutoscaleMin(false);
            //gd.setAltYGrid(true);
            gd.setAltYMrtg(true);
            //gd.setAntiAliasing(true);
            //gd.setBase(1.0);
            //gd.setDrawXGrid(true);
            //gd.setDrawYGrid(true);
            //gd.setForceRulesLegend(true);
            //gd.setInterlaced(true);
            //gd.setLogarithmic(true);
            //gd.setMaxValue(MAX_BANDWIDTH);
            //gd.setMinValue(0.0);
            //gd.setPoolUsed(true);
            //gd.setRigid(true);
            //gd.setShowSignature(true);
            //gd.setTextAntiAliasing(true);
            //gd.setUnit("unit");
        gd.setImageFormat("png");
        try {
            RrdGraph graph = new RrdGraph(gd);
        } catch (IOException ex) {
        }
    }

    public static RrdDef dbPrepare(String pathDB) {
        // !!!!!! - fixed step=60L !!!!!!!
        RrdDef rrdDef = new RrdDef(pathDB, 60L);
        rrdDef.addDatasource(DSinp, DsType.COUNTER, heartbeat, 0.0, MAX_BANDWIDTH);
        rrdDef.addDatasource(DSout, DsType.COUNTER, heartbeat, 0.0, MAX_BANDWIDTH);        
        rrdDef.addArchive(CF_AVE, 0.5, 1*KOF, 1440/KOF); // day   -     4*360=1440 min
        rrdDef.addArchive(CF_AVE, 0.5, 7*KOF, 1440/KOF); // week  -   7*4*360=10080 min
        rrdDef.addArchive(CF_AVE, 0.5,28*KOF, 1440/KOF); // month - 4*7*4*360=40320 min
        rrdDef.addArchive(CF_MAX, 0.5, 1*KOF, 1440/KOF); 
        rrdDef.addArchive(CF_MAX, 0.5, 7*KOF, 1440/KOF); 
        rrdDef.addArchive(CF_MAX, 0.5,28*KOF, 1440/KOF); 
        rrdDef.addArchive(CF_MIN, 0.5, 1*KOF, 1440/KOF); 
        rrdDef.addArchive(CF_MIN, 0.5, 7*KOF, 1440/KOF); 
        rrdDef.addArchive(CF_MIN, 0.5,28*KOF, 1440/KOF);      
        return rrdDef;
    }
    
    public static void main(String[] args) {
        //try (RrdDb rrdDb = new RrdDb(dbPrepare())) {
        try {
            END = Util.getTimestamp();//System.currentTimeMillis();//Util.getTimestamp();
            for (int i = 0; i < numfor; i++) {
                sample = rrdDb.createSample();
                input  = input  + 2*KOF*i*numfor*(2)*MAX_BANDWIDTH/99999;
                output = output + 2*KOF*i*numfor*(1)*MAX_BANDWIDTH/99999;
                //input  = input  + i*numfor*(2+Math.random())*MAX_BANDWIDTH/99999;
                //output = output + i*numfor*(1+Math.random())*MAX_BANDWIDTH/99999;                
                sample.setTime(END + i * 60L * KOF);
                sample.setValue(DSinp, input);
                sample.setValue(DSout, output);
                //sample.setAndUpdate(END + i * 60L + ":" + input);
                //sample.setAndUpdate(END + i * 60L + ":" + output);
                sample.update();
                System.out.println("@@@ = " + i + " step, DS-names = " + sample.getDsNames()[0]);
            }
            //sample.update();
        } catch (IOException ex) {
            Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
        //////////////////                      DAY
        rrdImgDraw("./img/loss-d.png", step_d, 1 * 86_400L, "Daily - "   +1*KOF+" min average");
        ///////////////////////                 WEEK
        rrdImgDraw("./img/loss-w.png", step_w, 7 * 86_400L, "Weekly - "  +7*KOF+" min average");
        ///////////////////////                 MONTH
        rrdImgDraw("./img/loss-m.png", step_m,28 * 86_400L, "Monthly - "+28*KOF+" min average");
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
            } catch (IOException ex) {
                Logger.getLogger(RrdSnmp.class.getName()).log(Level.SEVERE, null, ex);
            }
        });  
        //File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
        //System.out.println(jarDir.getAbsolutePath());
        Object[] ob = {zag, lbl01, lbl02, lbl03, jb};
        JOptionPane.showMessageDialog(null, ob, "RRD4j demo test guide", JOptionPane.CLOSED_OPTION, new ImageIcon("./lib/logo.png"));
    }

}
