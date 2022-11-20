package com.trodix.signature.security.utils;

public enum Claims {
    EMAIL("email"),;

    public final String value;

    private Claims(final String value) {
        this.value = value;
    }

    @Override 
    public String toString() { 
        return this.value; 
    }
}
