package de.bnass.RNAdConsent;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.HashMap;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import javax.annotation.Nonnull;

public class RNAdConsentModule extends ReactContextBaseJavaModule {
    private static final String E_ACTIVITY_NOT_AVAILABLE = "E_ACTIVITY_NOT_AVAILABLE";
    private static final String E_ACTIVITY_NOT_AVAILABLE_MSG = "Activity is not available.";

    private final ConsentInformation consentInformation;

    public RNAdConsentModule(ReactApplicationContext reactContext) {
        super(reactContext);
        consentInformation = UserMessagingPlatform.getConsentInformation(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "RNAdConsent";
    }


    private String getConsentStatusString(int consentStatus) {
        switch (consentStatus) {
            case ConsentInformation.ConsentStatus.REQUIRED:
                return "REQUIRED";
            case ConsentInformation.ConsentStatus.NOT_REQUIRED:
                return "NOT_REQUIRED";
            case ConsentInformation.ConsentStatus.OBTAINED:
                return "OBTAINED";
            case ConsentInformation.ConsentStatus.UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private String getPrivacyOptionsRequirementStatusString(
            ConsentInformation.PrivacyOptionsRequirementStatus privacyOptionsRequirementStatus) {
        switch (privacyOptionsRequirementStatus) {
            case REQUIRED:
                return "REQUIRED";
            case NOT_REQUIRED:
                return "NOT_REQUIRED";
            case UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private WritableMap getConsentInformation() {
        WritableMap consentStatusMap = Arguments.createMap();
        String consentStatus = getConsentStatusString(consentInformation.getConsentStatus());
        boolean isRequestLocationInEeaOrUnknown = !consentStatus.equals("NOT_REQUIRED") ;

        consentStatusMap.putString(
                "consentStatus", consentStatus);
        consentStatusMap.putBoolean("canRequestAds", consentInformation.canRequestAds());
        consentStatusMap.putString(
                "privacyOptionsRequirementStatus",
                getPrivacyOptionsRequirementStatusString(
                        consentInformation.getPrivacyOptionsRequirementStatus()));
        consentStatusMap.putBoolean(
                "isConsentFormAvailable", consentInformation.isConsentFormAvailable());
        consentStatusMap.putBoolean("isRequestLocationInEeaOrUnknown", isRequestLocationInEeaOrUnknown);
        return consentStatusMap;
    }


    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        final Map<String, Object> UMP_CONSENT_STATUS = new HashMap<>();
        UMP_CONSENT_STATUS.put("NOT_REQUIRED", ConsentInformation.ConsentStatus.NOT_REQUIRED);
        UMP_CONSENT_STATUS.put("OBTAINED", ConsentInformation.ConsentStatus.OBTAINED);
        UMP_CONSENT_STATUS.put("REQUIRED", ConsentInformation.ConsentStatus.REQUIRED);
        UMP_CONSENT_STATUS.put("UNKNOWN", ConsentInformation.ConsentStatus.UNKNOWN);
        constants.put("UMP_CONSENT_STATUS", UMP_CONSENT_STATUS);

        final Map<String, Object> UMP_DEBUG_GEOGRAPHY = new HashMap<>();
        UMP_DEBUG_GEOGRAPHY.put("DISABLED", ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED);
        UMP_DEBUG_GEOGRAPHY.put("EEA", ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);
        UMP_DEBUG_GEOGRAPHY.put("NOT_EEA", ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA);
        constants.put("UMP_DEBUG_GEOGRAPHY", UMP_DEBUG_GEOGRAPHY);

        final Map<String, Object> UMP_ERROR_CODE = new HashMap<>();
        UMP_ERROR_CODE.put("E_ACTIVITY_NOT_AVAILABLE", E_ACTIVITY_NOT_AVAILABLE);
        constants.put("UMP_ERROR_CODE", UMP_ERROR_CODE);

        final Map<String, Object> UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS = new HashMap<>();
        UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS.put("NOT_REQUIRED", ConsentInformation.PrivacyOptionsRequirementStatus.NOT_REQUIRED.ordinal());
        UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS.put("REQUIRED", ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED.ordinal());
        UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS.put("UNKNOWN", ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN.ordinal());
        constants.put("UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS", UMP_PRIVACY_OPTIONS_REQUIREMENT_STATUS);

        return constants;
    }

    @ReactMethod
    public void UMP_requestConsentInfoUpdate(@Nonnull final ReadableMap config, final Promise promise) {
        try {
            ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
            ConsentDebugSettings.Builder debugSettingsBuilder =
                    new ConsentDebugSettings.Builder(getReactApplicationContext().getApplicationContext());

            if (config.hasKey("testDeviceIds")) {
                ReadableArray testDeviceIds = config.getArray("testDeviceIds");
                int arraySize = testDeviceIds != null ? testDeviceIds.size() : 0;
                for (int i = 0; i < arraySize; i++) {
                    debugSettingsBuilder.addTestDeviceHashedId(testDeviceIds.getString(i));
                }
            }

            if (config.hasKey("debugGeography")) {
                debugSettingsBuilder.setDebugGeography(config.getInt("debugGeography"));
            }

            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build());

            if (config.hasKey("tagForUnderAgeOfConsent")) {
                paramsBuilder.setTagForUnderAgeOfConsent(config.getBoolean("tagForUnderAgeOfConsent"));
            }

            ConsentRequestParameters consentRequestParameters = paramsBuilder.build();

            Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                promise.reject(E_ACTIVITY_NOT_AVAILABLE, E_ACTIVITY_NOT_AVAILABLE_MSG);
                return;
            }

            consentInformation.requestConsentInfoUpdate(
                    currentActivity,
                    consentRequestParameters,
                    new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                        @Override
                        public void onConsentInfoUpdateSuccess() {
                            promise.resolve(getConsentInformation());
                        }
                    },
                    new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                        @Override
                        public void onConsentInfoUpdateFailure(FormError formError) {
                            promise.reject("" + formError.getErrorCode(), formError.getMessage());
                        }
                    }
            );
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void UMP_showConsentForm(final Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                promise.reject(E_ACTIVITY_NOT_AVAILABLE, E_ACTIVITY_NOT_AVAILABLE_MSG);
                return;
            }

            getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UserMessagingPlatform.loadConsentForm(
                            getReactApplicationContext().getApplicationContext(),
                            new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                                @Override
                                public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                                    consentForm.show(
                                            currentActivity,
                                            new ConsentForm.OnConsentFormDismissedListener() {
                                                @Override
                                                public void onConsentFormDismissed(@Nullable FormError formError) {
                                                    if (formError != null) {
                                                        promise.reject("" + formError.getErrorCode(), formError.getMessage());
                                                    } else {
                                                        promise.resolve(getConsentInformation());
                                                    }
                                                }
                                            });
                                }
                            },
                            new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                                @Override
                                public void onConsentFormLoadFailure(FormError formError) {
                                    promise.reject("" + formError.getErrorCode(), formError.getMessage());
                                }
                            }
                    );
                }
            });
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void UMP_reset() {
        consentInformation.reset();
    }
}
