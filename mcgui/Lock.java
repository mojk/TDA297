package mcgui;

/**
 *  Class containing an object that is used by the framework to
 *  synchronize everything in such a way that there are no
 *  simultaneous calls to a Multicaster thereby avoiding the need for
 *  synchronization within a Multicaster module.
 */
public class Lock {
    public final static Object lock = new Object();
}