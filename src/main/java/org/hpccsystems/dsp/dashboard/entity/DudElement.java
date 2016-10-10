package org.hpccsystems.dsp.dashboard.entity;

import java.io.Serializable;

import org.hpcc.HIPIE.dude.Element;

public class DudElement implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private Element inputElement;
    private Element outputElement;
    
    public Element getInputElement() {
        return inputElement;
    }

    public void setInputElement(Element inputElement) {
        this.inputElement = inputElement;
    }
    
    public Element getOutputElement() {
        return outputElement;
    }

    public void setOutputElement(Element dudOutputElement) {
        this.outputElement = dudOutputElement;
    }

}
