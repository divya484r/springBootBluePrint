package com.sample.phylon.exception;

import samplebackstopper.apierror.ApiError;
import samplebackstopper.apierror.ApiErrorBase;
import samplebackstopper.apierror.ApiErrorWithMetadata;
import samplebackstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import samplebackstopper.apierror.projectspecificinfo.sampleCommerceProjectApiErrorsBase;
import samplebackstopper.apierror.sample.SampleProjectApiErrorsBase;
import samplebackstopper.handler.ApiExceptionHandlerUtils;
import samplebackstopper.handler.listener.ApiExceptionHandlerListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Project-specific error definitions for this app. Note that the error codes for errors specified here must
 * conform to the range specified in {@link ProjectApiErrorsImpl#getProjectSpecificErrorCodeRange()} or an
 * exception will be thrown on app startup, and unit tests should fail. The one exception to this rule is a "core
 * error wrapper" - an instance that shares the same error code, message, and HTTP status code as a
 * {@link ProjectApiErrorsImpl#getCoreApiErrors()} instance (in this case that means a wrapper around a
 * {@link samplebackstopper.apierror.projectspecificinfo.sampleCommerceProjectApiErrorsBase} instance).
 *
 * <p>For more error creation and handling usage information please see the
 * <a href="https://github.com/sample-Inc/backstopper">Backstopper readme</a>.
 *
 * @author Nic Munroe
 */
public enum ProjectApiError implements ApiError {

    // Project-specific API Errors are defined here as enum values.
    EXAMPLE_API_ERROR_ENUM("FOO_ERROR", "Foo happened", 400);

    private final ApiError delegate;

    ProjectApiError(ApiError delegate) {
        this.delegate = delegate;
    }

    ProjectApiError(ApiError delegate, Map<String, Object> metadata) {
        this(new ApiErrorWithMetadata(delegate, metadata));
    }

    ProjectApiError(Object errorCode, String message, int httpStatusCode) {
        this(new ApiErrorBase(
                "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), String.valueOf(errorCode), message, httpStatusCode
        ));
    }

    ProjectApiError(Object errorCode, String message, int httpStatusCode, Map<String, Object> metadata) {
        this(new ApiErrorBase(
                "delegated-to-enum-wrapper-" + UUID.randomUUID().toString(), String.valueOf(errorCode), message, httpStatusCode, metadata
        ));
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getErrorCode() {
        return delegate.getErrorCode();
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public int getHttpStatusCode() {
        return delegate.getHttpStatusCode();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return delegate.getMetadata();
    }

    /**
     * Returns the project specific errors for this application. {@link #getProjectApiErrors()} will return a
     * combination of {@link SampleProjectApiErrorsBase#getCoreApiErrors()} and {@link #getProjectSpecificApiErrors()}.
     * This means that you have all the enum values of {@link samplebackstopper.apierror.sample.SampleCoreApiError}
     * and {@link ProjectApiError} at your disposal when throwing errors in this app.
     */
    @Named
    @Singleton
    public static class ProjectApiErrorsImpl extends sampleCommerceProjectApiErrorsBase {

        private static final List<ApiError> PROJECT_SPECIFIC_API_ERRORS = Arrays.asList(ProjectApiError.values());

        @Override
        protected List<ApiError> getProjectSpecificApiErrors() {
            return PROJECT_SPECIFIC_API_ERRORS;
        }

        @Override
        protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
            return ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES;
        }

    }
    /**
     * Spring config for supplying overrides to the Backstopper system.
     */
    @Configuration
    public static class ApplicationBackstopperOverrides {

        /**
         * A function that allows you to add/remove/replace the default list of {@link ApiExceptionHandlerListener}s.
         * Whatever is returned by this function is what will be used by Backstopper.
         */
        @Bean
        @Named("apiExceptionHandlerListenerCustomConfigurator")
        public Function<List<ApiExceptionHandlerListener>, List<ApiExceptionHandlerListener>> apiExceptionHandlerListenerCustomConfigurator() {
            return Function.identity();
        }

        /**
         * A function that allows you to modify or replace the default {@link ApiExceptionHandlerUtils}. Whatever is
         * returned by this function is what will be used by Backstopper.
         */
        @Bean
        @Named("apiExceptionHandlerUtilsCustomConfigurator")
        public Function<ApiExceptionHandlerUtils, ApiExceptionHandlerUtils> apiExceptionHandlerUtilsCustomConfigurator() {
            return Function.identity();
        }
    }

}
