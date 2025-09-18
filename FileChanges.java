diff --git a/activityservice/pom.xml b/activityservice/pom.xml
index b741c63..99a702e 100644
--- a/activityservice/pom.xml
+++ b/activityservice/pom.xml
@@ -59,6 +59,10 @@
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-webflux</artifactId>
         </dependency>
+        <dependency>
+            <groupId>org.springframework.kafka</groupId>
+            <artifactId>spring-kafka</artifactId>
+        </dependency>
     </dependencies>
     <dependencyManagement>
         <dependencies>
diff --git a/activityservice/src/main/java/com/fitness/activityservice/service/ActivityService.java b/activityservice/src/main/java/com/fitness/activityservice/service/ActivityService.java
index 535ed36..6ec01e2 100644
--- a/activityservice/src/main/java/com/fitness/activityservice/service/ActivityService.java
+++ b/activityservice/src/main/java/com/fitness/activityservice/service/ActivityService.java
@@ -5,6 +5,8 @@ import com.fitness.activityservice.dto.ActivityResponse;
 import com.fitness.activityservice.model.Activity;
 import com.fitness.activityservice.repository.ActivityRepository;
 import lombok.RequiredArgsConstructor;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.kafka.core.KafkaTemplate;
 import org.springframework.stereotype.Service;
 
 @Service
@@ -13,6 +15,11 @@ public class ActivityService {
 
     private final ActivityRepository activityRepository;
     private final UserValidationService userValidationService;
+    private final KafkaTemplate<String, Activity> kafkaTemplate;
+
+    @Value("${kafka.topic.name}")
+    private String topicName;
+
 
     public ActivityResponse trackActivity(ActivityRequest request) {
         Boolean isValidUser = userValidationService.validateUser(request.getUserId());
@@ -21,6 +28,11 @@ public class ActivityService {
         }
         Activity activity = Activity.builder().userId(request.getUserId()).type(request.getType()).duration(request.getDuration()).caloriesBurned(request.getCaloriesBurned()).startTime(request.getStartTime()).additionalMetrics(request.getAdditionalMetrics()).build();
         Activity savedActivity = activityRepository.save(activity);
+        try {
+            kafkaTemplate.send(topicName, savedActivity.getUserId(), savedActivity);
+        } catch (Exception e) {
+            e.printStackTrace();
+        }
         return mapToResponse(savedActivity);
     }
 
diff --git a/activityservice/src/main/resources/application.yml b/activityservice/src/main/resources/application.yml
index 8e1282b..7fc740d 100644
--- a/activityservice/src/main/resources/application.yml
+++ b/activityservice/src/main/resources/application.yml
@@ -5,10 +5,19 @@ spring:
     mongodb:
       uri: mongodb://localhost:27017/aiactivityfitness
       database: aiactivityfitness
+  kafka:
+    bootstrap-servers: localhost:9092
+    producer:
+      key-serializer: org.apache.kafka.common.serialization.StringSerializer
+      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
 
 server:
   port: 8082
 
+kafka:
+  topic:
+    name: activity-events
+
 eureka:
   client:
     service-url:
diff --git a/aiservice/src/main/java/com/fitness/aiservice/model/Activity.java b/aiservice/src/main/java/com/fitness/aiservice/model/Activity.java
new file mode 100644
index 0000000..5527e7d
--- /dev/null
+++ b/aiservice/src/main/java/com/fitness/aiservice/model/Activity.java
@@ -0,0 +1,37 @@
+package com.fitness.activityservice.model;
+
+import lombok.AllArgsConstructor;
+import lombok.Builder;
+import lombok.Data;
+import lombok.NoArgsConstructor;
+import org.springframework.data.annotation.CreatedDate;
+import org.springframework.data.annotation.LastModifiedDate;
+import org.springframework.data.mongodb.core.mapping.Document;
+import org.springframework.data.mongodb.core.mapping.Field;
+
+import java.time.LocalDateTime;
+import java.util.Map;
+
+@Document(collation = "activities")
+@Data
+@Builder
+@AllArgsConstructor
+@NoArgsConstructor
+public class Activity {
+
+    private String id;
+    private String userId;
+    private ActivityType type;
+    private Integer duration;
+    private Integer caloriesBurned;
+    private LocalDateTime startTime;
+
+    @Field("metrics")
+    private Map<String, Object> additionalMetrics;
+
+    @CreatedDate
+    private LocalDateTime createdAt;
+
+    @LastModifiedDate
+    private LocalDateTime updatedAt;
+}
diff --git a/aiservice/src/main/java/com/fitness/aiservice/model/ActivityType.java b/aiservice/src/main/java/com/fitness/aiservice/model/ActivityType.java
new file mode 100644
index 0000000..41ca832
--- /dev/null
+++ b/aiservice/src/main/java/com/fitness/aiservice/model/ActivityType.java
@@ -0,0 +1,5 @@
+package com.fitness.activityservice.model;
+
+public enum ActivityType {
+    RUNNING, WALKING, CYCLING, SWIMMING
+}
