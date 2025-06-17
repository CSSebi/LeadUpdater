import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) throws IOException {
        // Trello API und Around Home API
        // Die folgenden Werte werden jetzt aus Umgebungsvariablen gelesen
        String trelloApiKey = System.getenv("TRELLO_API_KEY");
        String trelloToken = System.getenv("TRELLO_TOKEN");
        String trelloIdList = System.getenv("TRELLO_ID_LIST");

        String aroundHomeClientId = System.getenv("AROUND_HOME_CLIENT_ID");
        String aroundHomeClientSecret = System.getenv("AROUND_HOME_CLIENT_SECRET");

        // Überprüfe, ob die Umgebungsvariablen gesetzt sind
        if (trelloApiKey == null || trelloToken == null || trelloIdList == null ||
                aroundHomeClientId == null || aroundHomeClientSecret == null) {
            System.err.println("Fehler: Eine oder mehrere notwendige Umgebungsvariablen sind nicht gesetzt.");
            System.err.println("Stellen Sie sicher, dass TRELLO_API_KEY, TRELLO_TOKEN, TRELLO_ID_LIST, AROUND_HOME_CLIENT_ID und AROUND_HOME_CLIENT_SECRET gesetzt sind.");
            System.exit(1);
        }

        String trelloUrlString = "https://api.trello.com/1/lists/" + trelloIdList + "/cards?key=" + trelloApiKey + "&token=" + trelloToken;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String apiUrl = "https://leads.around.technology/v201805/leads";

        URL url2 = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url2.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-CLIENT-ID", aroundHomeClientId);
        connection.setRequestProperty("X-CLIENT-SECRET", aroundHomeClientSecret);

        if (connection.getResponseCode() != 200) {
            System.err.println("Fehler beim Abrufen der Leads von Around Home API um " + timestamp);
            System.err.println("HTTP Response Code: " + connection.getResponseCode());
            System.exit(1);  // Fehlercode 1 für Bashskript
        }

        // Datenbankverbindung
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Fehler: Eine oder mehrere Datenbank-Umgebungsvariablen sind nicht gesetzt.");
            System.err.println("Stellen Sie sicher, dass DB_URL, DB_USER und DB_PASSWORD gesetzt sind.");
            System.exit(1);
        }


        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray leads = jsonResponse.getJSONArray("leads");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            System.out.println("Verbindung zur Datenbank erfolgreich!");
            //SQL Statement
            String insertSQL = """
                INSERT INTO leads (
                    idleads, bought, reachability, appointment, callingNotes, 
                    appointmentSet, notes, startsAt, endsAt, address, zip_code, city, 
                    company, salutation, offerContactFirstName, offerContactLastName, 
                    offerAddress, offerZipCode, offerCity, email, phone, mobile, 
                    installationCompany, installationSalutation, installationFirstName, 
                    installationLastName, installationAddress, installationZipCode, 
                    installationCity, installationEmail, installationPhone, installationMobile
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

            // Lead-Daten durchlaufen
            for (int i = 0; i < leads.length(); i++) {
                JSONObject lead = leads.getJSONObject(i);
                // Key-Values extrahieren
                int idleads = lead.getInt("lead_id");
                String bought = lead.getString("bought_at");
                String leadId = String.valueOf(idleads);
                String reachability = lead.getString("reachability");
                String appointment = lead.getString("appointment");
                String callingNotes = lead.getString("calling_notes");
                JSONObject appointmentInformation = lead.getJSONObject("appointment_information");
                boolean appointmentSet = appointmentInformation.getBoolean("appointment_set");
                String notes = appointmentInformation.getString("notes");
                String startsAt = appointmentInformation.getString("starts_at");
                String endsAt = appointmentInformation.getString("ends_at");
                JSONObject location = appointmentInformation.getJSONObject("location");
                String address = location.getString("address");
                String zip_code = location.getString("zip_code");
                String city = location.getString("city");
                JSONObject offerContact = lead.getJSONObject("offer_contact");
                String company = offerContact.getString("company");
                String salutation = offerContact.getString("salutation");
                String offerContactFirstName = offerContact.getString("first_name");
                String offerContactLastName = offerContact.getString("last_name");
                String offerAddress = offerContact.getString("address");
                String zipCode = offerContact.getString("zip_code");
                String offerCity = offerContact.getString("city");
                String email = offerContact.getString("email");
                String phone = offerContact.getString("phone");
                String mobile = offerContact.getString("mobil");
                JSONObject installationContact = lead.getJSONObject("installation_contact");
                String installationCompany = installationContact.getString("company");
                String installationSalutation = installationContact.getString("salutation");
                String installationFirstName = installationContact.getString("first_name");
                String installationLastName = installationContact.getString("last_name");
                String installationAddress = installationContact.getString("address");
                String installationZipCode = installationContact.getString("zip_code");
                String installationCity = installationContact.getString("city");
                String installationEmail = installationContact.getString("email");
                String installationPhone = installationContact.getString("phone");
                String installationMobile = installationContact.getString("mobil");
                JSONArray answeredQuestions = lead.getJSONArray("answered_questions");

                // Fragen und Antworten formatieren
                StringBuilder questionsAndAnswers = new StringBuilder();
                for (int j = 0; j < answeredQuestions.length(); ++j) {
                    String question = answeredQuestions.getJSONObject(j).getString("question");
                    questionsAndAnswers.append("Frage: ").append(question).append("\\n");

                    for (int a = 0; a < answeredQuestions.getJSONObject(j).getJSONArray("answers").length(); ++a) {
                        String postData = answeredQuestions.getJSONObject(j).getJSONArray("answers").getJSONObject(a).getString("answer");
                        questionsAndAnswers.append("Antwort: ").append(postData).append("\\n\\n");
                    }
                }
                String questions = questionsAndAnswers.toString();

                // SQL-PreparedStatement setzen
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);
                pstmt.setInt(1, idleads);
                pstmt.setString(2, bought);
                pstmt.setString(3, reachability);
                pstmt.setString(4, appointment);
                pstmt.setString(5, callingNotes);
                pstmt.setBoolean(6, appointmentSet);
                pstmt.setString(7, notes);
                pstmt.setString(8, startsAt);
                pstmt.setString(9, endsAt);
                pstmt.setString(10, address);
                pstmt.setString(11, zip_code);
                pstmt.setString(12, city);
                pstmt.setString(13, company);
                pstmt.setString(14, salutation);
                pstmt.setString(15, offerContactFirstName);
                pstmt.setString(16, offerContactLastName);
                pstmt.setString(17, offerAddress);
                pstmt.setString(18, zipCode);
                pstmt.setString(19, offerCity);
                pstmt.setString(20, email);
                pstmt.setString(21, phone);
                pstmt.setString(22, mobile);
                pstmt.setString(23, installationCompany);
                pstmt.setString(24, installationSalutation);
                pstmt.setString(25, installationFirstName);
                pstmt.setString(26, installationLastName);
                pstmt.setString(27, installationAddress);
                pstmt.setString(28, installationZipCode);
                pstmt.setString(29, installationCity);
                pstmt.setString(30, installationEmail);
                pstmt.setString(31, installationPhone);
                pstmt.setString(32, installationMobile);

                // In die Datenbank einfügen
                pstmt.executeUpdate();
                pstmt.close();

                // POST-Anfrage an Trello senden
                String trelloPostData = "{\"name\":\"" + leadId + "\",\"desc\":\"ID: " + leadId + "\\nGekauft am: " + bought + "\\nErreichbarkeit: " + reachability + "\\nTermin: " + appointment + "\\nNotizen: " + callingNotes + "\\nTermin vereinbart : " + appointmentSet + "\\nTerminnotizen : " + notes + "\\nTermin beginnt am : " + startsAt + "\\nTermin endet am : " + endsAt + "\\nTermin Adresse : " + address + "\\nTermin Postleitzahl : " + zip_code + "\\nTermin Stadt : " + city + "\\n\\nAngebot Kontakt Firma : " + company + "\\nAngebot Kontakt Anrede : " + salutation + "\\nAngebot Kontakt Vorname : " + offerContactFirstName + "\\nAngebot Kontakt Nachname : " + offerContactLastName + "\\nAngebot Kontakt Adresse : " + offerAddress + "\\nAngebot Kontakt Postleitzahl : " + zipCode + "\\nAngebot Kontakt Stadt : " + offerCity + "\\nAngebot Kontakt Email : " + email + "\\nAngebot Kontakt Telefon : " + phone + "\\nAngebot Kontakt Mobiltelefon : " + mobile + "\\n\\nInstallation Kontakt Firma : " + installationCompany + "\\nInstallation Kontakt Anrede : " + installationSalutation + "\\nInstallation Kontakt Vorname : " + installationFirstName + "\\nInstallation Kontakt Nachname : " + installationLastName + "\\nInstallation Kontakt Adresse : " + installationAddress + "\\nInstallation Kontakt Postleitzahl : " + installationZipCode + "\\nInstallation Kontakt Stadt : " + installationCity + "\\nInstallation Kontakt Email : " + installationEmail + "\\nInstallation Kontakt Telefon : " + installationPhone + "\\nInstallation Kontakt Mobil : " + installationMobile + "\\n\\nFragen und Antworten:\\n" + questions + "\",\"idList\":\"" + trelloIdList + "\"}";

                try {
                    URL url3 = new URL(trelloUrlString);
                    HttpURLConnection conn2 = (HttpURLConnection) url3.openConnection();
                    conn2.setRequestMethod("POST");
                    conn2.setRequestProperty("Content-Type", "application/json");
                    conn2.setDoOutput(true);

                    // Senden der Anfrage
                    try (DataOutputStream outputStream = new DataOutputStream(conn2.getOutputStream())) {
                        outputStream.write(trelloPostData.getBytes(StandardCharsets.UTF_8));
                    }

                    // Prüfen des Response-Codes
                    if (conn2.getResponseCode() != 200) {
                        System.err.println("Fehler beim Senden an Trello für Lead " + leadId + " aufgetreten um " + timestamp);
                        System.err.println("HTTP Response Code: " + conn2.getResponseCode());
                        System.exit(1);  // Fehlercode 1 für Bashskript
                    } else {
                        System.out.println("Lead " + leadId + " erfolgreich in Datenbank eingefügt und an Trello gesendet.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("IOException beim Senden an Trello für Lead " + leadId + ".");
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Verbindung zur Datenbank fehlgeschlagen oder Fehler beim Einfügen von Daten.");
            System.exit(1);  // Fehler beim Verbinden zur Datenbank oder SQL-Fehler
        }
    }
}
