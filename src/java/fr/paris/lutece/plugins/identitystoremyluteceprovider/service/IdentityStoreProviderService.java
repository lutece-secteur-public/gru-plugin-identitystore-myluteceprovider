/*
 * Copyright (c) 2002-2013, Mairie de Paris
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AttributeDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.IdentitySearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.mylutece.business.LuteceUserAttributeDescription;
import fr.paris.lutece.plugins.mylutece.service.IMyLuteceExternalIdentityProviderService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;


/**
 *
 * IdentityStoreProviderService
 *
 */
@ApplicationScoped
public class IdentityStoreProviderService implements IMyLuteceExternalIdentityProviderService
{
	   // Properties for page titles
    private static final String PROPERTY_PROVIDER_ATTRIBUTE_DESCRIPTION_MESSAGE= "identitystoremyluteceprovider.provider.attributeDescriptionMessage";
 
	private static final String PROPERTY_IDENTITY_STORE_APPLICATION_CODE = "identitystoremyluteceprovider.identityStoreApplicationCode";
    private static final String LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX = "ids.";
    private static final String CERTIFICATE_INFORMATIONS_PREFIX = ".certificate.";
    private static final String CERTIFICATE_INFORMATION_CODE = "code";
    private static final String CERTIFICATE_INFORMATION_LEVEL = "level";
    
    
    
    
    private static final String HAS_IDS_ATTRIBUTE = "has_ids_attribute";
    private static final String CONSTANT_TRUE = "true";
    private static final String CONSTANT_FALSE = "false";

    @Inject
    @Named( IdentityStoreClientProducer.BEAN_IDENTITY_SERVICE )
    private IdentityService _identityStoreService;

    @Inject
    @ConfigProperty( name = PROPERTY_IDENTITY_STORE_APPLICATION_CODE )
    private Optional<String> _strClientAppCode;
    

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getIdentityInformations( String strName )
    {
        Map<String, String> UserInformations = new HashMap<String, String>(  );
        String strClientAppCode = _strClientAppCode.orElse( null );

        if ( StringUtils.isBlank( strClientAppCode ) )
        {
            AppLogService.info( "IdentityStoreProviderService: property {} is not set, identity store not queried", PROPERTY_IDENTITY_STORE_APPLICATION_CODE );
            return UserInformations;
        }

        try
        {
            IdentitySearchResponse response = _identityStoreService.getIdentityByConnectionId( strName, strClientAppCode,
                    new RequestAuthor( strClientAppCode, AuthorType.application.name( ) ) );
            IdentityDto identityDto = ( ( response != null ) && ( response.getIdentities( ) != null ) && !response.getIdentities( ).isEmpty( ) )
                    ? response.getIdentities( ).get( 0 ) : null;

            if ( ( identityDto != null ) && ( identityDto.getAttributes(  ) != null ) && !identityDto.getAttributes(  ).isEmpty( ) )
            {
                for ( AttributeDto attribute : identityDto.getAttributes(  ) )
                {
                    UserInformations.put( LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX + attribute.getKey(  ),
                        attribute.getValue(  ) );
                    if(attribute.isCertified())
                    {
                    	UserInformations.put( LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX + attribute.getKey(  ) + CERTIFICATE_INFORMATIONS_PREFIX+CERTIFICATE_INFORMATION_CODE,
                                attribute.getCertifier() );
                    	if ( attribute.getCertificationLevel( ) != null )
                    	{
                    		UserInformations.put( LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX + attribute.getKey(  ) + CERTIFICATE_INFORMATIONS_PREFIX+CERTIFICATE_INFORMATION_LEVEL,
                                    attribute.getCertificationLevel( ).toString( ) );
                    	}
                    }
                }
                UserInformations.put( LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX + HAS_IDS_ATTRIBUTE,
                        CONSTANT_TRUE );
            }
            if (  ( identityDto != null ) && ( identityDto.getAttributes(  ) != null ) && identityDto.getAttributes(  ).isEmpty( ) )
            {
                UserInformations.put( LUTECE_USER_ATTRIBUTE_IDENTITYSTORE_PREFIX + HAS_IDS_ATTRIBUTE,
                        CONSTANT_FALSE );
            }
        }
        catch ( IdentityStoreException | AppException infe )
        {
            AppLogService.error( "Error in IdentityStoreProviderService during loading identity for guid {}", strName, infe );
        }

        return UserInformations;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
   public List<LuteceUserAttributeDescription> getLuteceUserAttributesProvided(Locale locale)
    {
    	String strDescriptionMessage=I18nService.getLocalizedString(PROPERTY_PROVIDER_ATTRIBUTE_DESCRIPTION_MESSAGE, locale);
    	List<LuteceUserAttributeDescription>  luteceUserAttributeDescriptions=  IMyLuteceExternalIdentityProviderService.super.getLuteceUserAttributesProvided(locale);
    	luteceUserAttributeDescriptions.forEach(x->x.setDescription(strDescriptionMessage));
    	return luteceUserAttributeDescriptions;
    	
    	
    
    }
}
