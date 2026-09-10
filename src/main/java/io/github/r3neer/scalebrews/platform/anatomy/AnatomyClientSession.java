package io.github.r3neer.scalebrews.platform.anatomy;

/**
 * Connection-scoped catalog ownership plus an opaque client-level key.
 * The catalog survives a portal/world-level switch on the same connection;
 * poses, contacts and provider cursors deliberately do not.
 */
public final class AnatomyClientSession {
    private AnatomyCatalogTransfer catalog=new AnatomyCatalogTransfer();
    private Object level;

    public AnatomyCatalogTransfer catalog(){return catalog;}
    /** @return true only for a new level identity. */
    public boolean useLevel(Object nextLevel) {
        if(level==nextLevel)return false;
        level=nextLevel;return true;
    }
    public AnatomyMode mode(Object entityLevel) {
        return entityLevel==null || entityLevel!=level?AnatomyMode.DISABLED:catalog.mode();
    }
    /** Disconnect/host replacement, unlike a same-connection dimension switch. */
    public void resetConnection(){catalog=new AnatomyCatalogTransfer();level=null;}
}
