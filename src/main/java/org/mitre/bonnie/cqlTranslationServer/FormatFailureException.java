package org.mitre.bonnie.cqlTranslationServer;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class FormatFailureException extends WebApplicationException {

  private static final long serialVersionUID = 4299899582089710350L;

  public FormatFailureException(String msg) {
    super(Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(msg)
            .build());
  }

  public FormatFailureException(List<Exception> formatErrs) {
    super(Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(formatMsg(formatErrs))
            .build());
  }

  private static String formatMsg(List<Exception> formatErrs) {
    StringBuilder msg = new StringBuilder();
    msg.append("CQL formatting failed due to errors:");
    for (Exception error : formatErrs) {
      msg.append(error.getMessage()).append("\n");
    }
    return msg.toString();
  }
}
