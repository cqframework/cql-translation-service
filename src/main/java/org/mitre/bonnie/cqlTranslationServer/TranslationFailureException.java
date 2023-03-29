/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.bonnie.cqlTranslationServer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author mhadley
 */
public class TranslationFailureException extends WebApplicationException {

  private static final long serialVersionUID = 3188788471978609249L;

  public TranslationFailureException(String msg) {
    super(Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(msg)
            .build());
  }
}
