import 'package:flutter_dotenv/flutter_dotenv.dart';

class AppConfig {
  static String baseUrl = dotenv.env['API_BASE_URL'] ?? 'apu_url';

} 