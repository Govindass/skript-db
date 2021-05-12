package com.btk5h.skriptdb.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SQLQueryCompleteEvent extends Event {
    private final static HandlerList HANDLERS = new HandlerList();
    private String argument;

    public SQLQueryCompleteEvent(String argument) {
        super(true);
        this.argument = argument;
      //  this.variables = variables;
    }
    @Override
    public String getEventName() {
        return super.getEventName();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public String getQuery() {
        return argument;
    }

  //  public String getVariables() {return;}
}
