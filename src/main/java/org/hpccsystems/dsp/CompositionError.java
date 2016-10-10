package org.hpccsystems.dsp;

import java.util.Arrays;
import java.util.List;

import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.error.HError;

public class CompositionError {
    // List of fatal HIPIE errors
    static final int UNCAUGHT_EXCEPTION = 200;
    static final int SYSTEM_ERROR = 300;
    static final int FILE_NOT_FOUND = 400;
    static final int FILE_READ_ERROR = 401;
    static final int CONTRACT_NOT_FOUND = 501;
    static final int COMPOSITION_NOT_FOUND = 502;
    static final int DUPLICATE_CONTRACT = 503;
    static final int DUPLICATE_COMPOSITION = 504;
    static final int PERMISSION_DENIED = 1201;
    static final Integer[] TYPES_OF_ERROR = {
        UNCAUGHT_EXCEPTION, 
        SYSTEM_ERROR, 
        FILE_NOT_FOUND, 
        FILE_READ_ERROR, 
        CONTRACT_NOT_FOUND,
        COMPOSITION_NOT_FOUND, 
        DUPLICATE_CONTRACT, 
        DUPLICATE_COMPOSITION, 
        PERMISSION_DENIED 
        };

    private final boolean hasFatalError;
    private final boolean hasError;
    private final HError hError;
    
    public CompositionError(ErrorBlock errorBlock) {
        boolean hasFatalError = false;
        boolean hasError = false;
        HError hError = null;
        
        List<Integer> list = Arrays.asList(TYPES_OF_ERROR);
        for (HError error : errorBlock.getErrors()) {
            if(list.contains(error.getErrorCode().getNumVal())) {
                hError = error;
                hasFatalError = true;
                break;
            }
            hasError = true;
            hError = error;
        }
        
        this.hasFatalError = hasFatalError;
        this.hError = hError;
        this.hasError = hasError;
    }

    public HError gethError() {
        return hError;
    }

    public boolean hasError() {
        return hasError;
    }

    public boolean hasFatalError() {
        return hasFatalError;
    }
    
    public String getErrorMessage() {
        return hError.getErrorString();
    }
}
