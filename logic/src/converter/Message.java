package converter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Message {
    private List<FolderEvent> msg;
    private ReentrantLock l;

    public Message(){
        this.msg = new ArrayList<>();
        this.l = new ReentrantLock();
    }

    public FolderEvent getNextMsg() {
        while (l.isLocked()){}
        l.lock();
        FolderEvent fe = msg.get(0);
        msg.remove(0);
        l.unlock();
        return fe;
    }

    public Message addMsg(FolderEvent fe) {
        while (l.isLocked()){}
        l.lock();
        this.msg.add(fe);
        l.unlock();
        return this;
    }
    
    public boolean isEmpty() {
    	while (l.isLocked()){}
    	l.lock();
        boolean r = msg.isEmpty();
    	l.unlock();
    	return r;
    }
}
