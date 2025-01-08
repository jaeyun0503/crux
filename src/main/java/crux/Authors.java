package crux;

final class Authors {
  // TODO: Add author information.
  static final Author[] all = {new Author("Jenny Phan", "50035781", "phanjc1"),
          new Author("Jaeyun Kim", "44482235", "jaeyk14")};
}


final class Author {
  final String name;
  final String studentId;
  final String uciNetId;

  Author(String name, String studentId, String uciNetId) {
    this.name = name;
    this.studentId = studentId;
    this.uciNetId = uciNetId;
  }
}
