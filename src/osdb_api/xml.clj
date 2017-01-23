(ns osdb-api.xml)

(def log-in 
"<methodCall>
 <methodName>LogIn</methodName>
 <params>
  <param>
   <value><string>%s</string></value>
  </param>
  <param>
   <value><string>%s</string></value>
  </param>
  <param>
   <value><string>%s</string></value>
  </param>
  <param>
   <value><string>%s</string></value>
  </param>
 </params>
</methodCall>")

(def log-out
"<methodCall>
 <methodName>LogOut</methodName>
 <params>
  <param>
   <value><string>%s</string></value>
  </param>
 </params>
</methodCall>")

(def no-operation
"<methodCall>
 <methodName>NoOperation</methodName>
 <params>
  <param>
   <value><string>%s</string></value>
  </param>
 </params>
</methodCall>")
