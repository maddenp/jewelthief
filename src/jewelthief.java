// jewelthief
// Written by: Paul Madden (maddenp@colorado.edu) 2006
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty. You should have
// received a copy of the CC0 Public Domain Dedication along with this software.
// If not, see http://creativecommons.org/publicdomain/zero/1.0/.

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public final class jewelthief extends javax.swing.JFrame
{
  HashMap settings=new HashMap();
  jewelthiefengine engine;
  String referenceBaseURL="";
  JFrame outputFrame;
  JTextArea outputPanel;
  ArrayList fileTypes,fileTypeSelectors;
  HashMap<String,String> mimeTypes;

  public static void main(String args[])
  {
    jewelthief j=new jewelthief();
  }

  public jewelthief()
  {
    initComponents();
    setupFiletypeSelectorVector();
    setupMimetypeHashtable();
    setupOutputPanel();
    loadSettings();
    packSettings();
    log("gui ready\n");
    this.setVisible(true);
  }

  private void setupMimetypeHashtable()
  {
    mimeTypes=new HashMap();
    mimeTypes.put("jpg","image/jpeg");
    mimeTypes.put("gif","image/gif");
    mimeTypes.put("bmp","image/bmp");
    mimeTypes.put("mpg","video/mpeg");
    mimeTypes.put("avi","video/x-msvideo");
    mimeTypes.put("mov","video/quicktime");
    mimeTypes.put("mp3","audio/mpeg");
    mimeTypes.put("wav","audio/x-wav");
    mimeTypes.put("wmv","video/x-ms-wmv");
    mimeTypes.put("ogg","application/ogg");
    mimeTypes.put("png","image/png");
    mimeTypes.put("mid","audio/midi");
  }

  private void setupFiletypeSelectorVector()
  {
    int i=0;
    fileTypeSelectors=new ArrayList();
    fileTypeSelectors.add(getjpg);
    fileTypeSelectors.add(getgif);
    fileTypeSelectors.add(getbmp);
    fileTypeSelectors.add(getmpg);
    fileTypeSelectors.add(getavi);
    fileTypeSelectors.add(getmov);
    fileTypeSelectors.add(getmp3);
    fileTypeSelectors.add(getwav);
    fileTypeSelectors.add(getwmv);
    fileTypeSelectors.add(getogg);
    fileTypeSelectors.add(getpng);
    fileTypeSelectors.add(getmid);
    //filetypeSelectorVector.add(getall);
  }

  private void saveSettings()
  {
    packSettings();
    try
    {
      log("saving settings...");
      File settingsfile=new File("settings");
      boolean settingsfileexists=settingsfile.exists();
      FileOutputStream fos=new FileOutputStream("settings");
      ObjectOutputStream oos=new ObjectOutputStream(fos);
      oos.writeObject(settings);
      oos.close();
      if (settingsfileexists)
      {
        popUp("Saved!");
      }
      log("settings saved");
    }
    catch (Exception e)
    {
      log(" saving failed: "+e+"\n");
      File settingsfile=new File("settings");
      String fullname=settingsfile.getAbsolutePath();
      popUp("ERROR: Could not write to settings file\n"+fullname);
    }
  }

  private void loadSettings()
  {
    try
    {
      log("loading settings from file...");
      FileInputStream fis=new FileInputStream("settings");
      ObjectInputStream ois=new ObjectInputStream(fis);
      settings=(HashMap)ois.readObject();
      ois.close();
      url.setText((String)settings.get("baseurl"));
      ignoresmallfiles.setSelected(((Boolean)settings.get("checksize")).booleanValue());
      for (int i=0;i<fileTypeSelectors.size();i++)
      {
        JCheckBox jcb=(JCheckBox)fileTypeSelectors.get(i);
        jcb.setSelected(((Boolean)settings.get(jcb.getName())).booleanValue());
      }
      getall.setSelected(((Boolean)settings.get("getall")).booleanValue());
      setFiletypeState(!getall.isSelected());
      kbfield.setText(((Integer)settings.get("minimagesize")).toString());
      levels.setSelectedIndex(((Integer)settings.get("levels")).intValue());
      log(" load successful\n");
    }
    catch (Exception e)
    {
      log(" load failed: "+e+"\ndefault settings used\n");
      setFiletypeState(!getall.isSelected());
      saveSettings();
      popUp("Could not open settings file, loading defaults.");
    }
  }

  private void makeFiletypeVector()
  {
    fileTypes=new ArrayList();
    for (int i=0;i<fileTypeSelectors.size();i++)
    {
      JCheckBox jcb=(JCheckBox)fileTypeSelectors.get(i);
      if (jcb.isSelected())
      {
        fileTypes.add((String)mimeTypes.get(jcb.getText()));
      }
    }
  }

  private void setFiletypeState(boolean state)
  {
    for (int i=0;i<fileTypeSelectors.size();i++)
    {
      ((JCheckBox)fileTypeSelectors.get(i)).setEnabled(state);
    }
  }

  private void packSettings()
  {
    url.setText(url.getText().toLowerCase());
    settings.put("baseurl",url.getText());
    settings.put("checksize",ignoresmallfiles.isSelected());
    for (int i=0;i<fileTypeSelectors.size();i++)
    {
      JCheckBox jcb=(JCheckBox)fileTypeSelectors.get(i);
      settings.put(jcb.getName(),jcb.isSelected());
    }
    settings.put("getall",getall.isSelected());
    settings.put("minimagesize",new Integer(Integer.parseInt(kbfield.getText())));
    settings.put("levels",new Integer(levels.getSelectedIndex()));
  }

  void setupOutputPanel()
  {
    outputFrame=new JFrame();
    outputFrame.setSize(600,400);
    outputFrame.getContentPane().setLayout(new BorderLayout());
    outputPanel=new JTextArea();
    JScrollPane outputPane=new JScrollPane(outputPanel);
    Dimension d=outputFrame.getSize();
    outputPanel.setSize(d.width,d.height);
    outputFrame.getContentPane().add(outputPane,BorderLayout.CENTER);
    outputFrame.setVisible(false);
  }

  void log(String message)
  {
    //outputPanel.append(message+"\n");
    outputPanel.append(message);
    outputPanel.setCaretPosition(outputPanel.getDocument().getLength());
  }

  void popUp(String s)
  {
    javax.swing.JOptionPane.showMessageDialog(this,s);
  }

  void enableOptions(boolean enable)
  {
    if (!getall.isSelected())
    {
      setFiletypeState(enable);
    }
    getall.setEnabled(enable);
    ignoresmallfiles.setEnabled(enable);
    clearoutputbutton.setEnabled(enable);
    save.setEnabled(enable);
    exit.setEnabled(enable);
    levels.setEnabled(enable);
    kbfield.setEnabled(enable);
    url.setEnabled(enable);
  }

  final class jewelthiefengine extends Thread
  {
    ArrayList images=new ArrayList();
    String baseURL;
    boolean checkSize, stop=false, pause=false;
    int minImageSize, levels;

    public jewelthiefengine(String name)
    {
      super(name);
      log("engine instantiated\n");
      unpackSettings();
    }

    void unpackSettings()
    {
      baseURL=(String)settings.get("baseurl");
      log("base url: "+baseURL+"\n");
      checkSize=((Boolean)settings.get("checksize")).booleanValue();
      log("check object sizes: "+checkSize+"\n");
      minImageSize=(((Integer)settings.get("minimagesize")).intValue())*1000;
      log("min object size: "+minImageSize+"\n");
      levels=((Integer)settings.get("levels")).intValue();
      log("branch to "+levels+" forward level(s)"+"\n");
    }

    @Override
    public void run()
    {
      enableOptions(false);
      getPageLinks(baseURL,levels);
      progressbar.setValue(0);
      enableOptions(true);
    }

    private void pause()
    {
      try
      {
        jewelthiefengine.sleep(1000);
      }
      catch (Exception e)
      {
      }
      if (pause)
      {
        pause();
      }
    }

    void getPageLinks(String g,int level)
    {
      // extract the links found on this page
      log("DISCOVERING OBJECTS\n\n");
      progressbar.setIndeterminate(true);
      progressbar.setString("reading page");
      ArrayList links=extractLinks(g);
      log("finished discovering objects\n");
      if (stop)
      {
        return;
      }
      if (pause)
      {
        pause();
      }
      // loop through the links we've just extracted
      log("\nPROCESSING DISCOVERED OBJECTS\n");
      progressbar.setIndeterminate(false);
      progressbar.setString("getting objects");
      progressbar.setValue(0);
      int nlinks=links.size();
      progressbar.setMaximum(nlinks);
      for (int i=0;i<nlinks;i++)
      {
        if (stop)
        {
          return;
        }
        if (pause)
        {
          pause();
        }
        String link=(String)links.get(i);
        log("\nlink: "+link+"\n");
        // if we've already decided to download this object, no need to examine it again
        if (images.contains(link))
        {
          log("object already downloaded\n");
          continue;
        }
        progressbar.setValue(i);
        try
        {
          URL url=new URL(link);
          String host=url.getHost();
          String path=url.getPath();
          if (path.length()==0)
          {
            path="/";
          }
          String file=url.getFile();
          log("host: "+host+"\n");
          log("path: "+path+"\n");
          log("file: "+file+"\n");
          String currentBaseURL=host+path+"/";
          URLConnection uc=url.openConnection();
          // set referer to base url to emulate manual browsing
          uc.setRequestProperty("referer","http://"+host+"/");
          uc.connect();
          String type=uc.getContentType();
          log("type: "+type+"\n");
          // if the remote object is anything BUT an html file, download it
          if (type.indexOf("text/html")==-1)
          {
            // if the remote object is of unknow type, skip it
            if (!fileTypes.isEmpty())
            {
              if (type==null)
              {
                log("skipping null-type object\n");
                continue;
              }
              if (!fileTypes.contains(type))
              {
                log("type "+type+" not selected, ignoring object\n");
                continue;
              }
              log("type "+type+" is selected, downloading object\n");
            }
            int size=uc.getContentLength();
            if ((!checkSize)||((checkSize)&&(size>=minImageSize)))
            {
              String localFilename=link.replace('/','%');
              // strip off the http:%% prefix
              localFilename=localFilename.substring(7);
              log("local filename will be "+localFilename+"\n");
              if (!currentBaseURL.equals(referenceBaseURL))
              {
                referenceBaseURL=currentBaseURL;
              }
              if (stop)
              {
                return;
              }
              if (pause)
              {
                pause();
              }
              URLToFile(link,localFilename);
              images.add(link);
            }
            else
            {
              log("object too small: "+size+" < "+minImageSize+"\n");
            }
          }
          // if it IS an html document, recurse into it if we're not already at the deepest link level
          else
          {
            if (level>0)
            {
              log("\nbranching to handle "+link+"\n\n");
              getPageLinks(link,level-1);
            }
            else
            {
              log("current level is "+level+", skipping "+link+"\n");
            }
          }
        }
        catch (Exception e)
        {
          log("could not understand link: "+link+": "+e+"\n");
        }
      }
      log("\nFINISHED PROCESSING OBJECTS\n\n");
      progressbar.setString("finished");
    }

    ArrayList extractLinks(String g)
    {
      int punctPosition;
      String token, protocol, host, path, file, type;
      ArrayList links=new ArrayList();
      StringBuilder buffer=new StringBuilder();
      log("extracting links from "+g+"\n\n");
      try
      {
        // create url based on input, store components for later use
        URL url=new URL(g);
        protocol=url.getProtocol();
        log("protocol: "+protocol+"\n");
        host=url.getHost();
        log("host: "+host+"\n");
        path=url.getPath();
        file=url.getFile();
        /*
         * try { if (path.lastIndexOf("/")!=path.length())
         * path=path.substring(0,path.lastIndexOf("/")); } catch (Exception e) {
         * }
         */
        log("path: "+path+"\n");
        log("file: "+file+"\n");
        URLConnection uc=url.openConnection();
        // set referer to base url to emulate manual browsing
        uc.setRequestProperty("referer","http://"+url.getHost()+"/");
        log("connecting to "+g+"\n");
        uc.connect();
        log("connection successful\n");
        type=uc.getContentType();
        log("content type: "+type+"\n");
        // make sure this is an html document, skip if not
        if (type.indexOf("text/html")!=-1)
        {
          // get an input stream and read in the remote html file
          log("reading "+g+"\n");
          BufferedInputStream in=new BufferedInputStream(uc.getInputStream());
          for (;;)
          {
            if (stop)
            {
              return links;
            }
            if (pause)
            {
              pause();
            }
            int data=in.read();
            if (data==-1)
            {
              break;
            }
            else
            {
              buffer.append((char)data);
            }
          }
          log("reading complete\n");
        }
        else
        {
          log("expected html file, returning\n");
          return links; // if this was not an html file
        }
        // parse the html file for tags, process each tag
        StringTokenizer st=new StringTokenizer(buffer.toString(),"<>");
        log("parsing for links\n");
        while (st.hasMoreTokens())
        {
          if (stop)
          {
            return links;
          }
          if (pause)
          {
            pause();
          }
          // convert tag to lower case
          token=st.nextToken().toLowerCase();
          if ((token.indexOf("a href=")!=-1)||(token.indexOf("src=")!=-1))
          {
            log("\nlink : "+token);
            // strip off characters before http:
            punctPosition=token.indexOf("http:");
            if (punctPosition!=-1)  // this is an absolute url beginning with http
            {
              log(" (absolute)");
              // skip this link if the a href tag has no closing quote
              token=token.substring(punctPosition);
              punctPosition=token.indexOf("\"");
              if (punctPosition==-1)
              {
                log(": link not properly quoted\n");
                continue;
              }
              // strip off anything after closing quote in a href tag
              token=token.substring(0,punctPosition);
            }
            else // this is a relative url
            {
              log(" (relative)");
              // for forms like img name="x" src="x.gif"
              punctPosition=token.indexOf("src");
              if (punctPosition!=-1)
              {
                token=token.substring(punctPosition);
              }
              // strip off characters up to opening quote
              punctPosition=token.indexOf("\"");
              if (punctPosition==-1)
              {
                continue;
              }
              token=token.substring(++punctPosition);
              // strip off characters after and including closing quote
              punctPosition=token.indexOf("\"");
              if (punctPosition==-1)
              {
                continue;
              }
              token=token.substring(0,punctPosition);
              // make sure relative path starts with a slash
              if (token.indexOf("/")!=0)
              {
                token="/"+token;
              }
              // build url composed of base url info + relative path
              token=protocol+"://"+host+path+token;
            }
            // strip off any extraneous % ? & directives
            punctPosition=token.indexOf("%");
            if (punctPosition!=-1)
            {
              token=token.substring(0,punctPosition);
            }
            punctPosition=token.indexOf("?");
            if (punctPosition!=-1)
            {
              token=token.substring(0,punctPosition);
            }
            punctPosition=token.indexOf("&");
            if (punctPosition!=-1)
            {
              token=token.substring(0,punctPosition);
            }
            log(", clean form: "+token);
            // make sure we don't have this link already, add to vector
            if (!links.contains(token))
            {
              links.add(token);
              log(" -- scheduled");
            }
            else
            {
              log(" -- already scheduled");
            }
          }
        }
      }
      catch (Exception e)
      {
      }
      return links;
    }

    void URLToFile(String targetURL,String fileName)
    {
      log("downloading "+targetURL+"\n");
      try
      {
        URL url=new URL(targetURL);
        URLConnection uc=url.openConnection();
        uc.setRequestProperty("referer","http://"+url.getHost()+"/");
        uc.connect();
        BufferedInputStream in=new BufferedInputStream(uc.getInputStream());
        BufferedOutputStream out=new BufferedOutputStream(new FileOutputStream(fileName));
        for (;;)
        {
          int data=in.read();
          if (data==-1)
          {
            break;
          }
          else
          {
            out.write(data);
          }
        }
        out.close();
      }
      catch (Exception e)
      {
        log("download failed: "+e+"\n");
      }
    }
  }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titlePanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        mainConstraintPanel = new javax.swing.JPanel();
        main = new javax.swing.JPanel();
        urlConstraintPanel = new javax.swing.JPanel();
        urlPanel = new javax.swing.JPanel();
        url = new javax.swing.JTextField();
        levelsConstraintPanel = new javax.swing.JPanel();
        levelsPanel = new javax.swing.JPanel();
        labelsText1 = new javax.swing.JLabel();
        levels = new javax.swing.JComboBox();
        levels.addItem("0");
        levels.addItem("1");
        levels.addItem("2");
        levels.setSelectedIndex(0);
        levelsText2 = new javax.swing.JLabel();
        filetypesPanel = new javax.swing.JPanel();
        filetypes = new javax.swing.JPanel();
        getall = new javax.swing.JCheckBox();
        getavi = new javax.swing.JCheckBox();
        getbmp = new javax.swing.JCheckBox();
        getgif = new javax.swing.JCheckBox();
        getjpg = new javax.swing.JCheckBox();
        getmid = new javax.swing.JCheckBox();
        getmov = new javax.swing.JCheckBox();
        getmp3 = new javax.swing.JCheckBox();
        getmpg = new javax.swing.JCheckBox();
        getogg = new javax.swing.JCheckBox();
        getpng = new javax.swing.JCheckBox();
        getwav = new javax.swing.JCheckBox();
        getwmv = new javax.swing.JCheckBox();
        optionsPanel = new javax.swing.JPanel();
        options = new javax.swing.JPanel();
        ignoreobjectspanel = new javax.swing.JPanel();
        ignoresmallfiles = new javax.swing.JCheckBox();
        kbfield = new javax.swing.JTextField();
        kblabel = new javax.swing.JLabel();
        buttonsPanel = new javax.swing.JPanel();
        showoutputbutton = new javax.swing.JButton();
        clearoutputbutton = new javax.swing.JButton();
        save = new javax.swing.JButton();
        exit = new javax.swing.JButton();
        transport = new javax.swing.JPanel();
        go = new javax.swing.JButton();
        pause = new javax.swing.JButton();
        stop = new javax.swing.JButton();
        progressPanel = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();

        setTitle("JewelThief");
        setFont(new java.awt.Font("SansSerif", 0, 8)); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        titlePanel.setFont(titlePanel.getFont());

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/title.png"))); // NOI18N
        titlePanel.add(jLabel1);

        getContentPane().add(titlePanel, java.awt.BorderLayout.NORTH);

        mainConstraintPanel.setFont(mainConstraintPanel.getFont());
        mainConstraintPanel.setLayout(new java.awt.BorderLayout());

        main.setBorder(null);
        main.setFont(main.getFont());
        main.setLayout(new javax.swing.BoxLayout(main, javax.swing.BoxLayout.Y_AXIS));

        urlConstraintPanel.setFont(urlConstraintPanel.getFont());

        urlPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Search this URL", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        urlPanel.setFont(urlPanel.getFont());
        urlPanel.setLayout(new java.awt.BorderLayout());

        url.setFont(url.getFont());
        url.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        url.setPreferredSize(new java.awt.Dimension(300, 21));
        url.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                urlActionPerformed(evt);
            }
        });
        urlPanel.add(url, java.awt.BorderLayout.CENTER);

        urlConstraintPanel.add(urlPanel);

        main.add(urlConstraintPanel);

        levelsConstraintPanel.setFont(levelsConstraintPanel.getFont());

        levelsPanel.setFont(levelsPanel.getFont());
        levelsPanel.setLayout(new javax.swing.BoxLayout(levelsPanel, javax.swing.BoxLayout.LINE_AXIS));

        labelsText1.setFont(labelsText1.getFont());
        labelsText1.setText("and search pages ");
        levelsPanel.add(labelsText1);

        levels.setFont(levels.getFont());
        levels.setMaximumRowCount(3);
        levels.setMaximumSize(new java.awt.Dimension(40, 32767));
        levels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                levelsActionPerformed(evt);
            }
        });
        levelsPanel.add(levels);

        levelsText2.setFont(levelsText2.getFont());
        levelsText2.setText("level(s) deeper");
        levelsText2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6));
        levelsPanel.add(levelsText2);

        levelsConstraintPanel.add(levelsPanel);

        main.add(levelsConstraintPanel);

        filetypesPanel.setFont(filetypesPanel.getFont());

        filetypes.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "for these file types", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        filetypes.setFont(filetypes.getFont());
        filetypes.setLayout(new java.awt.GridLayout(0, 5));

        getall.setFont(getall.getFont().deriveFont(getall.getFont().getSize()-4f));
        getall.setSelected(true);
        getall.setText("ALL");
        getall.setBorder(null);
        getall.setMaximumSize(new java.awt.Dimension(50, 22));
        getall.setMinimumSize(new java.awt.Dimension(50, 22));
        getall.setName("getall");
        getall.setPreferredSize(new java.awt.Dimension(50, 22));
        getall.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getallItemStateChanged(evt);
            }
        });
        filetypes.add(getall);

        getavi.setFont(getavi.getFont().deriveFont(getavi.getFont().getSize()-4f));
        getavi.setText("avi");
        getavi.setBorder(null);
        getavi.setMaximumSize(new java.awt.Dimension(50, 22));
        getavi.setMinimumSize(new java.awt.Dimension(50, 22));
        getavi.setName("getavi");
        getavi.setPreferredSize(new java.awt.Dimension(50, 22));
        getavi.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getaviItemStateChanged(evt);
            }
        });
        filetypes.add(getavi);

        getbmp.setFont(getbmp.getFont().deriveFont(getbmp.getFont().getSize()-4f));
        getbmp.setText("bmp");
        getbmp.setBorder(null);
        getbmp.setMaximumSize(new java.awt.Dimension(50, 22));
        getbmp.setMinimumSize(new java.awt.Dimension(50, 22));
        getbmp.setName("getbmp");
        getbmp.setPreferredSize(new java.awt.Dimension(50, 22));
        getbmp.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getbmpItemStateChanged(evt);
            }
        });
        filetypes.add(getbmp);

        getgif.setFont(getgif.getFont().deriveFont(getgif.getFont().getSize()-4f));
        getgif.setText("gif");
        getgif.setBorder(null);
        getgif.setMaximumSize(new java.awt.Dimension(50, 22));
        getgif.setMinimumSize(new java.awt.Dimension(50, 22));
        getgif.setName("getgif");
        getgif.setPreferredSize(new java.awt.Dimension(50, 22));
        getgif.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getgifItemStateChanged(evt);
            }
        });
        filetypes.add(getgif);

        getjpg.setFont(getjpg.getFont().deriveFont(getjpg.getFont().getSize()-4f));
        getjpg.setText("jpg");
        getjpg.setBorder(null);
        getjpg.setMaximumSize(new java.awt.Dimension(50, 22));
        getjpg.setMinimumSize(new java.awt.Dimension(50, 22));
        getjpg.setName("getjpg");
        getjpg.setPreferredSize(new java.awt.Dimension(50, 22));
        getjpg.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getjpgItemStateChanged(evt);
            }
        });
        filetypes.add(getjpg);

        getmid.setFont(getmid.getFont().deriveFont(getmid.getFont().getSize()-4f));
        getmid.setText("mid");
        getmid.setBorder(null);
        getmid.setMaximumSize(new java.awt.Dimension(50, 22));
        getmid.setMinimumSize(new java.awt.Dimension(50, 22));
        getmid.setName("getmid");
        getmid.setPreferredSize(new java.awt.Dimension(50, 22));
        filetypes.add(getmid);

        getmov.setFont(getmov.getFont().deriveFont(getmov.getFont().getSize()-4f));
        getmov.setText("mov");
        getmov.setBorder(null);
        getmov.setMaximumSize(new java.awt.Dimension(50, 22));
        getmov.setMinimumSize(new java.awt.Dimension(50, 22));
        getmov.setName("getmov");
        getmov.setPreferredSize(new java.awt.Dimension(50, 22));
        getmov.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getmovItemStateChanged(evt);
            }
        });
        filetypes.add(getmov);

        getmp3.setFont(getmp3.getFont().deriveFont(getmp3.getFont().getSize()-4f));
        getmp3.setText("mp3");
        getmp3.setBorder(null);
        getmp3.setMaximumSize(new java.awt.Dimension(50, 22));
        getmp3.setMinimumSize(new java.awt.Dimension(50, 22));
        getmp3.setName("getmp3");
        getmp3.setPreferredSize(new java.awt.Dimension(50, 22));
        getmp3.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getmp3ItemStateChanged(evt);
            }
        });
        filetypes.add(getmp3);

        getmpg.setFont(getmpg.getFont().deriveFont(getmpg.getFont().getSize()-4f));
        getmpg.setText("mpg");
        getmpg.setBorder(null);
        getmpg.setMaximumSize(new java.awt.Dimension(50, 22));
        getmpg.setMinimumSize(new java.awt.Dimension(50, 22));
        getmpg.setName("getmpg");
        getmpg.setPreferredSize(new java.awt.Dimension(50, 22));
        getmpg.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getmpgItemStateChanged(evt);
            }
        });
        filetypes.add(getmpg);

        getogg.setFont(getogg.getFont().deriveFont(getogg.getFont().getSize()-4f));
        getogg.setText("ogg");
        getogg.setBorder(null);
        getogg.setMaximumSize(new java.awt.Dimension(50, 22));
        getogg.setMinimumSize(new java.awt.Dimension(50, 22));
        getogg.setName("getogg");
        getogg.setPreferredSize(new java.awt.Dimension(50, 22));
        filetypes.add(getogg);

        getpng.setFont(getpng.getFont().deriveFont(getpng.getFont().getSize()-4f));
        getpng.setText("png");
        getpng.setBorder(null);
        getpng.setMaximumSize(new java.awt.Dimension(50, 22));
        getpng.setMinimumSize(new java.awt.Dimension(50, 22));
        getpng.setName("getpng");
        getpng.setPreferredSize(new java.awt.Dimension(50, 22));
        filetypes.add(getpng);

        getwav.setFont(getwav.getFont().deriveFont(getwav.getFont().getSize()-4f));
        getwav.setText("wav");
        getwav.setBorder(null);
        getwav.setMaximumSize(new java.awt.Dimension(50, 22));
        getwav.setMinimumSize(new java.awt.Dimension(50, 22));
        getwav.setName("getwav");
        getwav.setPreferredSize(new java.awt.Dimension(50, 22));
        getwav.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                getwavItemStateChanged(evt);
            }
        });
        filetypes.add(getwav);

        getwmv.setFont(getwmv.getFont().deriveFont(getwmv.getFont().getSize()-4f));
        getwmv.setText("wmv");
        getwmv.setBorder(null);
        getwmv.setMaximumSize(new java.awt.Dimension(50, 22));
        getwmv.setMinimumSize(new java.awt.Dimension(50, 22));
        getwmv.setName("getwmv");
        getwmv.setPreferredSize(new java.awt.Dimension(50, 22));
        filetypes.add(getwmv);

        filetypesPanel.add(filetypes);

        main.add(filetypesPanel);

        optionsPanel.setFont(optionsPanel.getFont());
        optionsPanel.setMinimumSize(new java.awt.Dimension(200, 200));

        options.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "and", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        options.setFont(options.getFont());
        options.setLayout(new java.awt.GridLayout(1, 0));

        ignoreobjectspanel.setFont(ignoreobjectspanel.getFont());
        ignoreobjectspanel.setLayout(new javax.swing.BoxLayout(ignoreobjectspanel, javax.swing.BoxLayout.LINE_AXIS));

        ignoresmallfiles.setFont(ignoresmallfiles.getFont());
        ignoresmallfiles.setText("ignore items < ");
        ignoresmallfiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoresmallfilesActionPerformed(evt);
            }
        });
        ignoreobjectspanel.add(ignoresmallfiles);

        kbfield.setFont(kbfield.getFont().deriveFont(kbfield.getFont().getSize()-2f));
        kbfield.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        kbfield.setText("20");
        kbfield.setMaximumSize(new java.awt.Dimension(30, 21));
        kbfield.setMinimumSize(new java.awt.Dimension(30, 21));
        kbfield.setPreferredSize(new java.awt.Dimension(30, 21));
        kbfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                kbfieldActionPerformed(evt);
            }
        });
        ignoreobjectspanel.add(kbfield);

        kblabel.setFont(kblabel.getFont());
        kblabel.setText(" kb");
        ignoreobjectspanel.add(kblabel);

        options.add(ignoreobjectspanel);

        optionsPanel.add(options);

        main.add(optionsPanel);

        buttonsPanel.setMaximumSize(new java.awt.Dimension(120, 32767));
        buttonsPanel.setLayout(new java.awt.GridLayout(4, 0));

        showoutputbutton.setFont(showoutputbutton.getFont().deriveFont(showoutputbutton.getFont().getSize()-2f));
        showoutputbutton.setText("show log");
        showoutputbutton.setPreferredSize(new java.awt.Dimension(50, 23));
        showoutputbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showoutputbuttonActionPerformed(evt);
            }
        });
        buttonsPanel.add(showoutputbutton);

        clearoutputbutton.setFont(showoutputbutton.getFont());
        clearoutputbutton.setText("clear log");
        clearoutputbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearoutputbuttonActionPerformed(evt);
            }
        });
        buttonsPanel.add(clearoutputbutton);

        save.setFont(showoutputbutton.getFont());
        save.setText("save settings");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        buttonsPanel.add(save);

        exit.setFont(showoutputbutton.getFont());
        exit.setText("exit");
        exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitActionPerformed(evt);
            }
        });
        buttonsPanel.add(exit);

        main.add(buttonsPanel);

        transport.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 1, 1, 1));
        transport.setLayout(new javax.swing.BoxLayout(transport, javax.swing.BoxLayout.LINE_AXIS));

        go.setFont(go.getFont());
        go.setIcon(new javax.swing.ImageIcon(getClass().getResource("/start.png"))); // NOI18N
        go.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goActionPerformed(evt);
            }
        });
        transport.add(go);

        pause.setFont(pause.getFont());
        pause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pause.png"))); // NOI18N
        pause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseActionPerformed(evt);
            }
        });
        transport.add(pause);

        stop.setFont(stop.getFont());
        stop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stop.png"))); // NOI18N
        stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopActionPerformed(evt);
            }
        });
        transport.add(stop);

        main.add(transport);

        progressPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 10, 1));

        progressbar.setFont(progressbar.getFont().deriveFont(progressbar.getFont().getSize()-4f));
        progressbar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        progressbar.setMaximumSize(new java.awt.Dimension(200, 16));
        progressbar.setName("");
        progressbar.setPreferredSize(new java.awt.Dimension(200, 16));
        progressbar.setString("");
        progressbar.setStringPainted(true);
        progressPanel.add(progressbar);

        main.add(progressPanel);

        mainConstraintPanel.add(main, java.awt.BorderLayout.NORTH);

        getContentPane().add(mainConstraintPanel, java.awt.BorderLayout.WEST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clearoutputbuttonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearoutputbuttonActionPerformed
    {//GEN-HEADEREND:event_clearoutputbuttonActionPerformed
      outputPanel.setText("");
    }//GEN-LAST:event_clearoutputbuttonActionPerformed

    private void getwavItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getwavItemStateChanged
    {//GEN-HEADEREND:event_getwavItemStateChanged
    }//GEN-LAST:event_getwavItemStateChanged

    private void getmp3ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getmp3ItemStateChanged
    {//GEN-HEADEREND:event_getmp3ItemStateChanged
    }//GEN-LAST:event_getmp3ItemStateChanged

    private void getmovItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getmovItemStateChanged
    {//GEN-HEADEREND:event_getmovItemStateChanged
    }//GEN-LAST:event_getmovItemStateChanged

    private void getaviItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getaviItemStateChanged
    {//GEN-HEADEREND:event_getaviItemStateChanged
    }//GEN-LAST:event_getaviItemStateChanged

    private void getmpgItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getmpgItemStateChanged
    {//GEN-HEADEREND:event_getmpgItemStateChanged
    }//GEN-LAST:event_getmpgItemStateChanged

    private void getbmpItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getbmpItemStateChanged
    {//GEN-HEADEREND:event_getbmpItemStateChanged
    }//GEN-LAST:event_getbmpItemStateChanged

    private void getgifItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getgifItemStateChanged
    {//GEN-HEADEREND:event_getgifItemStateChanged
    }//GEN-LAST:event_getgifItemStateChanged

    private void getjpgItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getjpgItemStateChanged
    {//GEN-HEADEREND:event_getjpgItemStateChanged
    }//GEN-LAST:event_getjpgItemStateChanged

    private void getallItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_getallItemStateChanged
    {//GEN-HEADEREND:event_getallItemStateChanged
      if (getall.isSelected())
      {
        for (int i=0;i<fileTypeSelectors.size();i++)
        {
          ((JCheckBox)fileTypeSelectors.get(i)).setSelected(false);
        }
      }
      setFiletypeState(!getall.isSelected());
    }//GEN-LAST:event_getallItemStateChanged

    private void showoutputbuttonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showoutputbuttonActionPerformed
    {//GEN-HEADEREND:event_showoutputbuttonActionPerformed
      outputFrame.setVisible(true);
    }//GEN-LAST:event_showoutputbuttonActionPerformed

    private void urlActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_urlActionPerformed
    {//GEN-HEADEREND:event_urlActionPerformed
    }//GEN-LAST:event_urlActionPerformed

    private void levelsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_levelsActionPerformed
    {//GEN-HEADEREND:event_levelsActionPerformed
    }//GEN-LAST:event_levelsActionPerformed

    private void exitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exitActionPerformed
    {//GEN-HEADEREND:event_exitActionPerformed
      log("exit button pressed, exiting\n");
      System.exit(0);
    }//GEN-LAST:event_exitActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveActionPerformed
    {//GEN-HEADEREND:event_saveActionPerformed
      log("save button pressed\n");
      saveSettings();
    }//GEN-LAST:event_saveActionPerformed

    private void stopActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_stopActionPerformed
    {//GEN-HEADEREND:event_stopActionPerformed
      log("stop button pressed\n");
      if (engine!=null)
      {
        if (engine.pause==true)
        {
          engine.pause=false;
        }
        while (!engine.isAlive());
        engine.stop=true;
        log("engine stopped\n");
        while (engine.isAlive());
        enableOptions(true);
        log("options panels enabled\n");
        engine=null;
        log("engine killed\n");
      }
      progressbar.setIndeterminate(false);
      progressbar.setString("");
      progressbar.setValue(0);
      log("progress bar reset\n");
    }//GEN-LAST:event_stopActionPerformed

    private void pauseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_pauseActionPerformed
    {//GEN-HEADEREND:event_pauseActionPerformed
      log("pause button pressed\n");
      if ((engine!=null)&&(engine.isAlive()))
      {
        if (engine.pause==false)
        {
          engine.pause=true;
          progressbar.setIndeterminate(false);
          log("engine was running, paused\n");
        }
        else
        {
          engine.pause=false;
          progressbar.setIndeterminate(true);
          log("engine was paused, restarting\n");
        }
      }
    }//GEN-LAST:event_pauseActionPerformed

    private void goActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_goActionPerformed
    {//GEN-HEADEREND:event_goActionPerformed
      log("start button pressed\n");
      packSettings();
      makeFiletypeVector();
      if ((engine!=null)&&(engine.isAlive())&&(engine.pause))
      {
        engine.pause=false;
        log("engine was paused, restarting\n");
      }
      else
      {
        if (!url.getText().startsWith("http://"))
        {
          popUp("URLs must begin with http://");
          return;
        }
        engine=new jewelthiefengine("engine");
        engine.start();
        log("engine started\n");
      }
    }//GEN-LAST:event_goActionPerformed

    private void exitForm(java.awt.event.WindowEvent evt)//GEN-FIRST:event_exitForm
  {
    log("exiting\n");
    System.exit(0);
    }//GEN-LAST:event_exitForm

  private void ignoresmallfilesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ignoresmallfilesActionPerformed
  {//GEN-HEADEREND:event_ignoresmallfilesActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_ignoresmallfilesActionPerformed

  private void kbfieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_kbfieldActionPerformed
  {//GEN-HEADEREND:event_kbfieldActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_kbfieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton clearoutputbutton;
    private javax.swing.JButton exit;
    private javax.swing.JPanel filetypes;
    private javax.swing.JPanel filetypesPanel;
    private javax.swing.JCheckBox getall;
    private javax.swing.JCheckBox getavi;
    private javax.swing.JCheckBox getbmp;
    private javax.swing.JCheckBox getgif;
    private javax.swing.JCheckBox getjpg;
    private javax.swing.JCheckBox getmid;
    private javax.swing.JCheckBox getmov;
    private javax.swing.JCheckBox getmp3;
    private javax.swing.JCheckBox getmpg;
    private javax.swing.JCheckBox getogg;
    private javax.swing.JCheckBox getpng;
    private javax.swing.JCheckBox getwav;
    private javax.swing.JCheckBox getwmv;
    private javax.swing.JButton go;
    private javax.swing.JPanel ignoreobjectspanel;
    private javax.swing.JCheckBox ignoresmallfiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField kbfield;
    private javax.swing.JLabel kblabel;
    private javax.swing.JLabel labelsText1;
    private javax.swing.JComboBox levels;
    private javax.swing.JPanel levelsConstraintPanel;
    private javax.swing.JPanel levelsPanel;
    private javax.swing.JLabel levelsText2;
    private javax.swing.JPanel main;
    private javax.swing.JPanel mainConstraintPanel;
    private javax.swing.JPanel options;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JButton pause;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JButton save;
    private javax.swing.JButton showoutputbutton;
    private javax.swing.JButton stop;
    private javax.swing.JPanel titlePanel;
    private javax.swing.JPanel transport;
    private javax.swing.JTextField url;
    private javax.swing.JPanel urlConstraintPanel;
    private javax.swing.JPanel urlPanel;
    // End of variables declaration//GEN-END:variables
}