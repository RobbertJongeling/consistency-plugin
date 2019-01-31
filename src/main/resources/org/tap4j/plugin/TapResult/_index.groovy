// Namespaces
l = namespace("/lib/layout")
tap = namespace("/org/tap4j/plugin/tags")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
i = namespace("jelly:fmt")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")


l.layout(norefresh: "true", css: "/plugin/consistency/css/consistency.css") {
  st.include(it: it.owner, page: "sidepanel.jelly") 
  l.main-panel() {
    h1("Consistency Checks Results") 
    if(it.isEmptyTestSet()) 
    else{
      table(width: "100%") {
        tr() {
          td(width: "5%", "${it.testSets.size()} files") 
          td("${it.getTotal()} tests, ${it.passed} ok, ${it.failed} not ok, ${it.skipped} skipped, ${it.toDo} ToDo, ${it.bailOuts} Bail Out!.")
        }
      }
      it.testSets.each() { map -> 
        if(map.getTestSet().getPlan().isSkip()) {
          p("File:") {
            span(class: "underline") {
              a(href: "contents?f=${map.fileName}", map.fileName) 
            }
          }
        }
        else{
          p("File:") {
            span(class: "underline") {
              a(href: "contents?f=${map.fileName}", map.fileName) 
            }
          }
        }
        table(width: "100%", class: "tap") {
          tr() {
            th() 
            th("Number") 
            th("Description") 
            th("Directive") 
          }
          map.testSet.tapLines.each() { tapLine -> 
            tap.line(tapFile: map.fileName, tapLine: tapLine, "hello") 
          }
        }
        br() 
      }
    }
    if(it.hasParseErrors() == false) 
    else{
      h3("Parse errors") 
      table(width: "100%", class: "tap") {
        tr() {
          th("File name") 
          th("Cause") 
        }
        it.parseErrorTestSets.each() { testSet -> 
          tr() {
            td(testSet.fileName) 
            td(testSet.cause) 
          }
        }
      }
    }
  }
}
