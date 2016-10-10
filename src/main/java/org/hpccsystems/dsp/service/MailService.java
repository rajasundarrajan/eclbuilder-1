package org.hpccsystems.dsp.service;

import java.util.List;

import javax.mail.MessagingException;

import org.hpcc.HIPIE.Composition;

public interface MailService {
    
    void notifyReadyforTesting(List<Composition> compositions, List<String> recipients) throws MessagingException;
}
