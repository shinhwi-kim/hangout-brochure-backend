package com.hangoutbrochure.command.transform.source;

/** Turns one source's stored raw rows into public hangouts. */
public interface TransformSource {

    String sourceName();

    void transform();
}
