/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.identitystoremyluteceprovider.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.plugins.identitystore.v3.web.rs.service.HttpAccessTransport;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.service.HttpApiManagerAccessTransport;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.service.IdentityTransportRest;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Produces the identity store client used by the provider, from the plugin properties.
 */
@ApplicationScoped
public class IdentityStoreClientProducer
{
    public static final String BEAN_IDENTITY_SERVICE = "identitystoremyluteceprovider.identitystore.identityService";

    /**
     * Produces the identity store service over a REST transport, behind an API manager when its endpoint is set.
     *
     * @param strApiEndPointUrl
     *            the identity store API endpoint URL
     * @param strAccessManagerEndPointUrl
     *            the API manager token endpoint URL, blank for a direct access
     * @param strAccessManagerCredentials
     *            the API manager credentials
     * @return the identity store service
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_IDENTITY_SERVICE )
    public IdentityService produceIdentityService(
            @ConfigProperty( name = "identitystoremyluteceprovider.identitystore.apiEndPointUrl" ) Optional<String> strApiEndPointUrl,
            @ConfigProperty( name = "identitystoremyluteceprovider.identitystore.accessManagerEndPointUrl" ) Optional<String> strAccessManagerEndPointUrl,
            @ConfigProperty( name = "identitystoremyluteceprovider.identitystore.accessManagerCredentials" ) Optional<String> strAccessManagerCredentials )
    {
        HttpAccessTransport transport;
        if ( StringUtils.isNotBlank( strAccessManagerEndPointUrl.orElse( null ) ) )
        {
            HttpApiManagerAccessTransport apiManagerTransport = new HttpApiManagerAccessTransport( );
            apiManagerTransport.setAccessManagerEndPointUrl( strAccessManagerEndPointUrl.get( ) );
            apiManagerTransport.setAccessManagerCredentials( strAccessManagerCredentials.orElse( "" ) );
            transport = apiManagerTransport;
        }
        else
        {
            transport = new HttpAccessTransport( );
        }
        transport.setApiEndPointUrl( strApiEndPointUrl.orElse( "" ) );
        return new IdentityService( new IdentityTransportRest( transport ) );
    }
}
