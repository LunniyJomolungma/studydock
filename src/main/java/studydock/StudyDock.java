package studydock;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class StudyDock extends JPanel {
    private static final Color BG = new Color(16,24,37), PANEL = new Color(24,36,52), INK = new Color(234,242,250), MUTED = new Color(157,177,198), MINT = new Color(104,224,185), GOLD = new Color(249,197,108);
    private final TaskStore store;
    private List<Task> tasks;
    private final LocalDate fixedDate;
    private final boolean demo;
    private final JSpinner budget = new JSpinner(new SpinnerNumberModel(120,15,720,15));
    private final JCheckBox weekends = new JCheckBox("Include weekends", false);
    private final JTextField filter = new JTextField(18);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Task", "Subject", "Due", "Remaining", "Priority", "Status"},0) {
        @Override public boolean isCellEditable(int row,int column){return false;}
    };
    private final JTable table = new JTable(model);
    private final JPanel stats = new JPanel(new GridLayout(1,4,16,0));
    private final JPanel timeline = new JPanel();
    private final JLabel status = new JLabel();
    private List<Task> visible = List.of();
    private Planner.Plan plan;

    public StudyDock(List<Task> initial, TaskStore store, boolean demo, LocalDate fixedDate) {
        this.tasks = new ArrayList<>(initial); this.store = store; this.demo = demo; this.fixedDate = fixedDate;
        setLayout(new BorderLayout(0,22)); setBackground(BG); setBorder(new EmptyBorder(28,30,20,30));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(0,1,0,4)); titles.setOpaque(false);
        titles.add(label("STUDYDOCK  /  YOUR NEXT TWO WEEKS",12,MINT,Font.BOLD));
        titles.add(label("Make room for what matters.",30,INK,Font.BOLD));
        titles.add(label("A practical plan based on your deadlines and available study time.",14,MUTED,Font.PLAIN));
        header.add(titles,BorderLayout.CENTER);
        JButton add = button("+  Add task",MINT); add.addActionListener(e -> edit(null));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));right.setOpaque(false);right.add(add);header.add(right,BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0,24));top.setOpaque(false);top.add(header,BorderLayout.NORTH);stats.setOpaque(false);top.add(stats,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0,14));content.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));controls.setOpaque(false);
        controls.add(label("Daily budget",13,MUTED,Font.PLAIN));budget.setPreferredSize(new Dimension(76,32));controls.add(budget);controls.add(label("min",13,MUTED,Font.PLAIN));
        weekends.setOpaque(false);weekends.setForeground(INK);controls.add(weekends);
        controls.add(label("Search",13,MUTED,Font.PLAIN));filter.setPreferredSize(new Dimension(170,32));controls.add(filter);
        JButton export=button("Export plan",new Color(62,83,106));export.setForeground(INK);export.addActionListener(e->export());controls.add(export);content.add(controls,BorderLayout.NORTH);

        table.setRowHeight(44);table.setFont(new Font("SansSerif",Font.PLAIN,13));table.setBackground(PANEL);table.setForeground(INK);table.setGridColor(new Color(40,56,75));table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(47,79,97));table.setSelectionForeground(Color.WHITE);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,12));table.getTableHeader().setBackground(new Color(33,47,65));table.getTableHeader().setForeground(MUTED);table.getTableHeader().setPreferredSize(new Dimension(0,36));
        table.getColumnModel().getColumn(0).setPreferredWidth(270);table.getColumnModel().getColumn(1).setPreferredWidth(110);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col) {
                super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                setBorder(new EmptyBorder(0,10,0,8));
                if(!selected) {setBackground(row%2==0?PANEL:new Color(27,40,57));setForeground(col==5&&"Overdue".equals(value)?GOLD:col==5&&"Done".equals(value)?MINT:INK);}
                return this;
            }
        };table.setDefaultRenderer(Object.class,renderer);
        JScrollPane tableScroll = new JScrollPane(table);tableScroll.setColumnHeaderView(table.getTableHeader());tableScroll.setBorder(BorderFactory.createLineBorder(new Color(43,59,79)));tableScroll.getViewport().setBackground(PANEL);
        JPanel taskPanel = new JPanel(new BorderLayout(0,12));taskPanel.setOpaque(false);taskPanel.add(tableScroll,BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setOpaque(false);
        for(String action:List.of("Edit selected","Mark done","Delete")) {
            JButton b=button(action,new Color(48,64,84));b.setForeground(INK);actions.add(b);
            b.addActionListener(e->{Task selected=selected();if(selected==null)return;switch(action){case "Edit selected"->edit(selected);case "Mark done"->replace(selected,selected.completed());case "Delete"->{if(JOptionPane.showConfirmDialog(this,"Delete “"+selected.title()+"”?","Delete task",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)replace(selected,null);}}});
        }
        taskPanel.add(actions,BorderLayout.SOUTH);
        timeline.setLayout(new BoxLayout(timeline,BoxLayout.Y_AXIS));timeline.setBackground(PANEL);timeline.setBorder(new EmptyBorder(16,16,16,16));
        JScrollPane planScroll=new JScrollPane(timeline);planScroll.setBorder(BorderFactory.createLineBorder(new Color(43,59,79)));planScroll.getVerticalScrollBar().setUnitIncrement(20);
        JTabbedPane tabs=new JTabbedPane();tabs.setFont(new Font("SansSerif",Font.BOLD,14));tabs.addTab("Task board",taskPanel);tabs.addTab("14-day plan",planScroll);content.add(tabs,BorderLayout.CENTER);add(content,BorderLayout.CENTER);
        status.setForeground(MUTED);status.setFont(new Font("SansSerif",Font.PLAIN,12));add(status,BorderLayout.SOUTH);
        budget.addChangeListener(e->refresh());weekends.addActionListener(e->refresh());
        filter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){refreshTable();}public void removeUpdate(javax.swing.event.DocumentEvent e){refreshTable();}public void changedUpdate(javax.swing.event.DocumentEvent e){refreshTable();}});
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&selected()!=null)edit(selected());}});
        refresh();
    }
    private LocalDate today(){return fixedDate==null?LocalDate.now():fixedDate;}
    private static JLabel label(String text,int size,Color color,int style){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",style,size));l.setForeground(color);return l;}
    private static JButton button(String text,Color color){JButton b=new JButton(text);b.setBackground(color);b.setForeground(BG);b.setFocusPainted(false);b.setFont(new Font("SansSerif",Font.BOLD,13));b.setBorder(new EmptyBorder(10,14,10,14));return b;}
    private Task selected(){int row=table.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(this,"Select a task first.");return null;}return visible.get(row);}
    private void refresh(){
        plan=Planner.build(tasks,today(),14,(Integer)budget.getValue(),weekends.isSelected());
        stats.removeAll();
        metric("ACTIVE TASKS",Long.toString(tasks.stream().filter(t->!t.done()).count()),"Ready for your next session",INK);
        metric("SCHEDULED",plan.allocated()+" min","Allocated in the next 14 days",MINT);
        metric("UNALLOCATED",plan.unallocated()+" min","Review deadlines or daily budget",GOLD);
        metric("COMPLETED",Long.toString(tasks.stream().filter(Task::done).count()),"Small steps count",INK);
        refreshTable();refreshTimeline();
        status.setText((demo?"DEMO · changes stay in memory":"LOCAL · changes saved to your task file")+"    •    Earliest deadline first, priority breaks ties    •    "+today());
        revalidate();repaint();
    }
    private void metric(String title,String value,String hint,Color color){JPanel p=new JPanel(new GridLayout(3,1,0,7));p.setBackground(PANEL);p.setBorder(new EmptyBorder(18,18,18,18));p.add(label(title,11,MUTED,Font.BOLD));p.add(label(value,27,color,Font.BOLD));p.add(label(hint,11,MUTED,Font.PLAIN));stats.add(p);}
    private void refreshTable(){
        String query=filter.getText().strip().toLowerCase(Locale.ROOT);
        visible=tasks.stream().filter(t->(t.title()+" "+t.subject()).toLowerCase(Locale.ROOT).contains(query)).sorted(Comparator.comparing(Task::done).thenComparing(Task::due).thenComparing(Task::title)).toList();
        model.setRowCount(0);
        for(Task t:visible)model.addRow(new Object[]{t.title(),t.subject(),t.due(),t.minutes()+" min",t.priorityName(),t.done()?"Done":t.due().isBefore(today())?"Overdue":t.due().equals(today())?"Due today":"Open"});
    }
    private void refreshTimeline(){
        timeline.removeAll();
        if(!plan.shortfalls().isEmpty()){
            timeline.add(label("NEEDS ATTENTION",12,GOLD,Font.BOLD));timeline.add(Box.createVerticalStrut(10));
            for(var s:plan.shortfalls()){JLabel l=label(s.task().title()+" — "+s.minutes()+" min: "+s.reason(),12,GOLD,Font.PLAIN);timeline.add(l);}
            timeline.add(Box.createVerticalStrut(24));
        }
        for(var day:plan.used().entrySet()){
            JPanel p=new JPanel(new BorderLayout(12,8));p.setOpaque(false);p.setBorder(new EmptyBorder(10,0,10,0));
            p.add(label(day.getKey().format(DateTimeFormatter.ofPattern("EEE, dd MMM",Locale.ENGLISH)),14,INK,Font.BOLD),BorderLayout.NORTH);
            JPanel entries=new JPanel(new GridLayout(0,1,0,5));entries.setOpaque(false);
            for(var block:plan.blocks())if(block.date().equals(day.getKey()))entries.add(label(block.minutes()+" min   ·   "+block.title()+"   /   "+block.subject(),13,MUTED,Font.PLAIN));
            if(entries.getComponentCount()==0)entries.add(label("Available for a break or new work",13,MUTED,Font.PLAIN));
            p.add(entries,BorderLayout.CENTER);JProgressBar bar=new JProgressBar(0,(Integer)budget.getValue());bar.setValue(day.getValue());bar.setStringPainted(true);bar.setString(day.getValue()+" / "+budget.getValue()+" min");bar.setForeground(MINT);bar.setBackground(BG);bar.setPreferredSize(new Dimension(150,24));p.add(bar,BorderLayout.EAST);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE,p.getPreferredSize().height));timeline.add(p);
        }
    }
    private void edit(Task original){
        JTextField title=new JTextField(original==null?"":original.title()),subject=new JTextField(original==null?"":original.subject()),due=new JTextField((original==null?today().plusDays(3):original.due()).toString());
        JSpinner minutes=new JSpinner(new SpinnerNumberModel(original==null?60:original.minutes(),0,6000,15));
        JComboBox<String> priority=new JComboBox<>(new String[]{"Low","Normal","High"});priority.setSelectedIndex(original==null?1:original.priority()-1);
        JPanel form=new JPanel(new GridLayout(0,2,12,12));for(var pair:new Object[][]{{"Title",title},{"Subject",subject},{"Deadline (YYYY-MM-DD)",due},{"Remaining minutes (0 = done)",minutes},{"Priority",priority}}){form.add(new JLabel(pair[0].toString()));form.add((Component)pair[1]);}
        while(JOptionPane.showConfirmDialog(this,form,original==null?"New task":"Edit task",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{minutes.commitEdit();Task t=new Task(original==null?UUID.randomUUID():original.id(),title.getText(),subject.getText(),LocalDate.parse(due.getText().strip()),(Integer)minutes.getValue(),priority.getSelectedIndex()+1);if(replace(original,t))return;}
            catch(Exception e){JOptionPane.showMessageDialog(this,"Check the fields: "+e.getMessage(),"Invalid task",JOptionPane.ERROR_MESSAGE);}
        }
    }
    private boolean replace(Task original,Task replacement){
        List<Task> changed=new ArrayList<>(tasks);
        if(original!=null)changed.removeIf(t->t.id().equals(original.id()));
        if(replacement!=null)changed.add(replacement);
        try{if(store!=null)store.save(changed);tasks=changed;refresh();return true;}
        catch(IOException e){JOptionPane.showMessageDialog(this,e.getMessage(),"Could not save; changes were not applied",JOptionPane.ERROR_MESSAGE);return false;}
    }
    private void export(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("study-plan.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path destination=chooser.getSelectedFile().toPath();
        if(Files.exists(destination)&&JOptionPane.showConfirmDialog(this,"Replace existing file?","Export",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{PlanExport.write(plan,destination);status.setText("Exported plan to "+destination.toAbsolutePath());}
        catch(IOException e){JOptionPane.showMessageDialog(this,e.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}
    }
    private static void layoutTree(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layoutTree(container);}
    public static void main(String[] args){
        boolean demo=Arrays.asList(args).contains("--demo");
        boolean render=args.length==2&&args[0].equals("--render-preview");
        if(render)System.setProperty("java.awt.headless","true");
        if(!(args.length==0||(args.length==1&&demo)||render)){System.err.println("Usage: StudyDock [--demo | --render-preview output.png]");System.exit(1);}
        SwingUtilities.invokeLater(()->{
            TaskStore opened=null;
            try{
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("TabbedPane.background",PANEL);UIManager.put("TabbedPane.foreground",INK);UIManager.put("TabbedPane.selected",new Color(48,69,89));UIManager.put("TabbedPane.contentAreaColor",BG);UIManager.put("TabbedPane.focus",MINT);
                if(render){
                    StudyDock panel=new StudyDock(Demo.tasks(LocalDate.of(2026,10,6)),null,true,LocalDate.of(2026,10,6));panel.setSize(1220,820);layoutTree(panel);
                    BufferedImage image=new BufferedImage(1220,820,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);panel.printAll(g);g.dispose();ImageIO.write(image,"png",Path.of(args[1]).toFile());return;
                }
                if(!demo)opened=new TaskStore(Path.of("data","tasks.tsv"));
                final TaskStore store=opened;
                StudyDock panel=new StudyDock(demo?Demo.tasks(LocalDate.now()):store.load(),store,demo,null);
                JFrame frame=new JFrame("StudyDock"+(demo?" — Demo":""));frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);frame.setContentPane(panel);frame.setSize(1220,820);frame.setMinimumSize(new Dimension(1060,700));frame.setLocationRelativeTo(null);
                frame.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){try{if(store!=null)store.close();}catch(IOException error){System.err.println(error.getMessage());}}});
                frame.setVisible(true);
            }catch(Exception e){if(opened!=null)try{opened.close();}catch(IOException ignored){}if(render){e.printStackTrace();System.exit(1);}else JOptionPane.showMessageDialog(null,e.getMessage(),"StudyDock could not start",JOptionPane.ERROR_MESSAGE);}
        });
    }
}
