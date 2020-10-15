package org.sonarsource.kotlin.plugin.surefire.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UnitTestIndex {

  private Map<String, UnitTestClassReport> indexByClassname;

  public UnitTestIndex() {
    this.indexByClassname = new HashMap<>();
  }

  public UnitTestClassReport index(String classname) {
    return indexByClassname.computeIfAbsent(classname, name -> new UnitTestClassReport());
  }

  public UnitTestClassReport get(String classname) {
    return indexByClassname.get(classname);
  }

  public Set<String> getClassnames() {
    return new HashSet<>(indexByClassname.keySet());
  }

  public Map<String, UnitTestClassReport> getIndexByClassname() {
    return indexByClassname;
  }

  public int size() {
    return indexByClassname.size();
  }

  public UnitTestClassReport merge(String classname, String intoClassname) {
    UnitTestClassReport from = indexByClassname.get(classname);
    if (from!=null) {
      UnitTestClassReport to = index(intoClassname);
      to.add(from);
      indexByClassname.remove(classname);
      return to;
    }
    return null;
  }

  public void remove(String classname) {
    indexByClassname.remove(classname);
  }


}
