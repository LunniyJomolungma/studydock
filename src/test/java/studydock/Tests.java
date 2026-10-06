package studydock;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class Tests {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Task task(String name,LocalDate due,int minutes,int priority){return new Task(UUID.randomUUID(),name,"Test",due,minutes,priority);}
    public static void main(String[] args)throws Exception{
        LocalDate monday=LocalDate.of(2026,10,5);
        Task first=task("Early",monday,150,1),second=task("Later",monday.plusDays(1),90,3);
        var plan=Planner.build(List.of(second,first),monday,2,120,true);
        check(plan.allocated()==210&&plan.unallocated()==30,"Deadline allocation incorrect");
        check(plan.shortfalls().getFirst().task().id().equals(first.id()),"Wrong overloaded task");
        check(plan.blocks().stream().allMatch(b->b.minutes()<=60&&b.minutes()>0),"Invalid block size");
        Task high=task("High",monday,90,3),low=task("Low",monday,90,1);
        var tie=Planner.build(List.of(low,high),monday,1,90,true);check(tie.blocks().getFirst().taskId().equals(high.id()),"Priority tie break failed");
        var weekend=Planner.build(List.of(task("Weekend",monday.plusDays(6),60,2)),monday.plusDays(5),2,120,false);
        check(weekend.allocated()==0&&weekend.unallocated()==60,"Weekend exclusion failed");
        var overdue=Planner.build(List.of(task("Late",monday.minusDays(1),30,2),task("Done",monday,0,2)),monday,14,120,true);
        check(overdue.allocated()==0&&overdue.unallocated()==30,"Overdue/completed handling failed");
        for(int seed=0;seed<100;seed++){
            Random random=new Random(seed);List<Task> list=new ArrayList<>();for(int i=0;i<30;i++)list.add(task("Task "+i,monday.plusDays(random.nextInt(20)-3),random.nextInt(300),1+random.nextInt(3)));
            var p=Planner.build(list,monday,14,90,seed%2==0);
            check(p.allocated()+p.unallocated()==list.stream().mapToInt(Task::minutes).sum(),"Work was lost or duplicated");
            check(p.used().values().stream().allMatch(v->v<=90),"Daily budget exceeded");
            Map<UUID,Task> byId=new HashMap<>();for(Task t:list)byId.put(t.id(),t);
            check(p.blocks().stream().allMatch(b->!b.date().isAfter(byId.get(b.taskId()).due())&&!b.date().isBefore(monday)),"Scheduled after deadline");
        }
        Path directory=Files.createTempDirectory("studydock-test-");Path file=directory.resolve("tasks.tsv");
        try{
            List<Task> original=List.of(new Task(UUID.randomUUID(),"Разобрать графы, \"BFS\"","Алгоритмы",monday,90,3));
            try(TaskStore store=new TaskStore(file)){
                store.save(original);check(store.load().equals(original),"Unicode round trip failed");
                boolean locked=false;try(TaskStore other=new TaskStore(file)){other.load();}catch(java.io.IOException e){locked=true;}check(locked,"Concurrent store allowed");
                Files.writeString(file,"STUDYDOCK\t1\nbroken\n");boolean rejected=false;try{store.load();}catch(java.io.IOException e){rejected=true;}check(rejected,"Corrupted file accepted");check(Files.readString(file).contains("broken"),"Corrupted file was overwritten");
            }
            Path csv=directory.resolve("plan.csv");PlanExport.write(Planner.build(original,monday,1,120,true),csv);check(Files.readString(csv).contains("\"\"BFS\"\""),"CSV quote escaping failed");
            PlanExport.write(Planner.build(List.of(task("=1+1",monday,30,1)),monday,1,120,true),csv);check(Files.readString(csv).contains("'=1+1"),"CSV formula protection failed");Files.delete(csv);
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(directory.resolve("tasks.tsv.lock"));Files.delete(directory);}
        System.out.println("All StudyDock tests passed; 100 schedule invariants and storage checks verified.");
    }
}
