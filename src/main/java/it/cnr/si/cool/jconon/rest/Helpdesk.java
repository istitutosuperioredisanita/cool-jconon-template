/*
 *    Copyright (C) 2019  Consiglio Nazionale delle Ricerche
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.cnr.si.cool.jconon.rest;

import it.cnr.cool.cmis.service.CMISService;
import it.cnr.cool.mail.MailService;
import it.cnr.cool.mail.model.AttachmentBean;
import it.cnr.cool.mail.model.EmailMessage;
import it.cnr.cool.security.SecurityChecked;
import it.cnr.cool.util.StringUtil;
import it.cnr.cool.web.scripts.exception.ClientMessageException;
import it.cnr.si.cool.jconon.model.HelpdeskBean;
import it.cnr.si.cool.jconon.service.helpdesk.HelpdeskService;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Created by cirone on 27/10/2014.
 */
@Path("helpdesk")
@Component
@Produces(MediaType.APPLICATION_JSON)
@SecurityChecked
public class Helpdesk {

    private static final Logger LOGGER = LoggerFactory.getLogger(Helpdesk.class);

    @Autowired
    private HelpdeskService helpdeskService;
    @Autowired
    private CMISService cmisService;
    @Autowired
    private CommonsMultipartResolver resolver;
    @Autowired
    private MailService mailService;

    @Inject
    private Environment env;


    /*@GET
    @Path("/esperti")
    public Response esperti(@Context HttpServletRequest req, @QueryParam("idCategoria") Integer idCategoria) {
    	return Response.ok(helpdeskService.getEsperti(idCategoria)).build();
    }

    @PUT
    @Path("/esperti")
    public Response addEsperto(@Context HttpServletRequest req, @QueryParam("idCategoria") Integer idCategoria, @QueryParam("idEsperto") String idEsperto) {
    	return Response.ok(helpdeskService.manageEsperto(idCategoria, idEsperto, false)).build();
    }

    @DELETE
    @Path("/esperti")
    public Response deleteEsperto(@Context HttpServletRequest req, @QueryParam("idCategoria") Integer idCategoria, @QueryParam("idEsperto") String idEsperto) {
    	return Response.ok(helpdeskService.manageEsperto(idCategoria, idEsperto, true)).build();
    }*/

    @POST
    @Path("/send")
    public Response send(@Context HttpServletRequest req) {

        MultipartHttpServletRequest mRequest = resolver.resolveMultipart(req);
        ResponseBuilder builder = null;

        HelpdeskBean hdBean = new HelpdeskBean();
        hdBean.setIp(req.getRemoteAddr());

        try {
            final Map<String, String[]> parameterMap = mRequest.getParameterMap();
            blockXSS(parameterMap.values()
                    .stream()
                    .map(strings -> Arrays.asList(strings))
                    .flatMap(List::stream)
                    .collect(Collectors.toList())
            );
            BeanUtils.populate(hdBean, parameterMap);
            String idSegnalazione;

            EmailMessage message = new EmailMessage();

            message.setSender(hdBean.getEmail());

            if(hdBean.getProblemType().equals("Problema Tecnico"))
                message.setRecipients(Arrays.asList(env.getProperty("helpdesk-mail.technical")));
            else if(hdBean.getProblemType().equals("Problema Normativo"))
                message.setRecipients(Arrays.asList(env.getProperty("helpdesk-mail.regulatory")));

            message.setSubject(hdBean.getFirstName() + " " + hdBean.getLastName() + " - " + hdBean.getCall() +
                    ", " + hdBean.getProblemType() + ": " + hdBean.getSubject());

            String body = hdBean.getMessage();
            if(hdBean.getPhoneNumber()!=null)
                body = body + "<br><br> Recapito telefonico: " + hdBean.getPhoneNumber();

            message.setBody(body);

            if(mRequest.getFileMap().get("allegato")!=null) {
                message.setAttachments(Arrays.asList(new AttachmentBean(mRequest
                        .getFileMap().get("allegato").getOriginalFilename(), mRequest
                        .getFileMap().get("allegato").getBytes())));
            }
            mailService.send(message);

            /*
            if (mRequest.getParameter("id") != null && mRequest.getParameter("azione") != null) {

                helpdeskService.sendReopenMessage(hdBean, cmisService
                        .getCMISUserFromSession(req));
            } else {
                helpdeskService.post(hdBean, mRequest
                        .getFileMap().get("allegato"), cmisService
                                             .getCMISUserFromSession(req));
            }*/

            builder = Response.ok();

        } catch (ClientMessageException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap("message", e.getMessage()));
        } catch (IllegalAccessException | InvocationTargetException | IOException | MailException | CmisObjectNotFoundException exception) {
            LOGGER.error("helpdesk send error", exception);
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage());
        }
        return builder.build();
    }

    private void blockXSS(List<String> values) {
        values
                .stream()
                .forEach(s -> {
                    for (Pattern scriptPattern : StringUtil.patternsXSS){
                        if (scriptPattern.matcher(s).find()) {
                            throw new ClientMessageException("message.error.caller");
                        }
                    }

                });
    }
}
