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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AttributeDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.IdentitySearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.mylutece.service.IMyLuteceExternalIdentityProviderService;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.ReflectionTestUtils;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tests the identity store provider.
 */
public class IdentityStoreProviderServiceTest extends LuteceTestCase
{
    private static final String CONNECTION_ID = "connection-id";
    private static final String CLIENT_CODE = "TEST_APP";

    /**
     * Identity service returning a fixed search response and recording the request.
     */
    private static final class StubIdentityService extends IdentityService
    {
        private final IdentitySearchResponse _response;
        private final boolean _bFail;
        private String _strConnectionId;
        private String _strClientCode;
        private RequestAuthor _author;

        /**
         * Builds the stub.
         *
         * @param response
         *            the response to return
         * @param bFail
         *            true to throw an identity store exception
         */
        StubIdentityService( IdentitySearchResponse response, boolean bFail )
        {
            _response = response;
            _bFail = bFail;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IdentitySearchResponse getIdentityByConnectionId( String strConnectionId, String strClientCode, RequestAuthor author )
                throws IdentityStoreException
        {
            _strConnectionId = strConnectionId;
            _strClientCode = strClientCode;
            _author = author;
            if ( _bFail )
            {
                throw new IdentityStoreException( "unreachable" );
            }
            return _response;
        }
    }

    /**
     * Builds a provider wired on the given identity service.
     *
     * @param identityService
     *            the identity service
     * @return the provider
     */
    private IdentityStoreProviderService provider( IdentityService identityService )
    {
        return provider( identityService, CLIENT_CODE );
    }

    /**
     * Builds a provider wired on the given identity service and client code.
     *
     * @param identityService
     *            the identity service
     * @param strClientCode
     *            the client code, null when not configured
     * @return the provider
     */
    private IdentityStoreProviderService provider( IdentityService identityService, String strClientCode )
    {
        IdentityStoreProviderService provider = new IdentityStoreProviderService( );
        ReflectionTestUtils.setField( provider, "_identityStoreService", identityService );
        ReflectionTestUtils.setField( provider, "_strClientAppCode", Optional.ofNullable( strClientCode ) );
        return provider;
    }

    /**
     * Builds a search response holding one identity with the given attributes.
     *
     * @param attributes
     *            the attributes
     * @return the response
     */
    private IdentitySearchResponse response( List<AttributeDto> attributes )
    {
        IdentityDto identity = new IdentityDto( );
        identity.setAttributes( attributes );
        IdentitySearchResponse response = new IdentitySearchResponse( );
        List<IdentityDto> identities = new ArrayList<>( );
        identities.add( identity );
        response.setIdentities( identities );
        return response;
    }

    /**
     * Builds an attribute.
     *
     * @param strKey
     *            the key
     * @param strValue
     *            the value
     * @param strCertifier
     *            the certifier code, null when not certified
     * @param nLevel
     *            the certification level
     * @return the attribute
     */
    private AttributeDto attribute( String strKey, String strValue, String strCertifier, Integer nLevel )
    {
        AttributeDto attribute = new AttributeDto( );
        attribute.setKey( strKey );
        attribute.setValue( strValue );
        attribute.setCertifier( strCertifier );
        attribute.setCertificationLevel( nLevel );
        attribute.setCertificationDate( strCertifier != null ? new Date( ) : null );
        return attribute;
    }

    /**
     * The provider is discovered by mylutece through CDI.
     */
    @Test
    public void testProviderIsDiscovered( )
    {
        assertTrue( CDI.current( ).select( IMyLuteceExternalIdentityProviderService.class ).stream( )
                .anyMatch( IdentityStoreProviderService.class::isInstance ), "provider not discovered" );
    }

    /**
     * Attributes and certificates are mapped with the ids prefix.
     */
    @Test
    public void testAttributesMapped( )
    {
        List<AttributeDto> attributes = new ArrayList<>( );
        attributes.add( attribute( "family_name", "Dupont", "fccertifier", 600 ) );
        attributes.add( attribute( "email", "a@b.fr", null, null ) );
        attributes.add( attribute( "gender", "1", "fccertifier", null ) );
        StubIdentityService identityService = new StubIdentityService( response( attributes ), false );

        Map<String, String> map = provider( identityService ).getIdentityInformations( CONNECTION_ID );

        assertEquals( CONNECTION_ID, identityService._strConnectionId, "connection id" );
        assertEquals( CLIENT_CODE, identityService._strClientCode, "client code" );
        assertEquals( CLIENT_CODE, identityService._author.getName( ), "author name" );
        assertEquals( "application", identityService._author.getType( ).name( ), "author type" );
        assertEquals( "Dupont", map.get( "ids.family_name" ), "value" );
        assertEquals( "a@b.fr", map.get( "ids.email" ), "value" );
        assertEquals( "fccertifier", map.get( "ids.family_name.certificate.code" ), "certifier" );
        assertEquals( "600", map.get( "ids.family_name.certificate.level" ), "level" );
        assertFalse( map.containsKey( "ids.email.certificate.code" ), "uncertified attribute" );
        assertEquals( "fccertifier", map.get( "ids.gender.certificate.code" ), "certifier without level" );
        assertFalse( map.containsKey( "ids.gender.certificate.level" ), "no level" );
        assertEquals( "true", map.get( "ids.has_ids_attribute" ), "has attributes" );
    }

    /**
     * An identity without attributes is flagged.
     */
    @Test
    public void testIdentityWithoutAttributes( )
    {
        Map<String, String> map = provider( new StubIdentityService( response( new ArrayList<>( ) ), false ) ).getIdentityInformations( CONNECTION_ID );

        assertEquals( 1, map.size( ), "size" );
        assertEquals( "false", map.get( "ids.has_ids_attribute" ), "has attributes" );
    }

    /**
     * No identity or an unreachable store gives an empty map.
     */
    @Test
    public void testNoIdentity( )
    {
        assertTrue( provider( new StubIdentityService( new IdentitySearchResponse( ), false ) ).getIdentityInformations( CONNECTION_ID ).isEmpty( ),
                "no identity" );
        assertTrue( provider( new StubIdentityService( null, true ) ).getIdentityInformations( CONNECTION_ID ).isEmpty( ), "store error" );
    }

    /**
     * Without client code the store is not queried.
     */
    @Test
    public void testNoClientCode( )
    {
        StubIdentityService identityService = new StubIdentityService( response( new ArrayList<>( ) ), false );

        assertTrue( provider( identityService, null ).getIdentityInformations( CONNECTION_ID ).isEmpty( ), "empty map" );
        assertNull( identityService._strConnectionId, "store not queried" );
    }
}
