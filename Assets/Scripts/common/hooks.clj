(ns common.hooks
  (:use arcadia.core common.core arcadia.linear)
  (:import [UnityEngine GameObject Camera Quaternion Vector3]))

;@todo change to namespaced keywords 
;      ex: :follow -> ::follow , :follow/scale -> ::follow.scale
;@todo think about mutually exclusive
;      perhaps it is better if is one follow at a time 
;      otherwise it could be its own game
(def ^:const enumap
  {:position 1
   :rotation 2 ;bleh
   :pose 3 ;position|rotation
   :shoulder 4})

;@todo add some modifiers on how it follows, based on state
;      ex: translational speed, rotation speed, blocking view, etc
(def ^:const default-state
  {:follow/position (v3 0)
   :follow/rotation (qt)
   :follow/fov 60
   :follow/distance 0})
   
;@todo think about what happens when followed is destroyed  
(defn follow-position [^GameObject follower k]
  (if-let [^GameObject followed (state follower k)]
    (set! (.. follower transform position)
          (v3+ (.. followed transform position)
               (state follower :follow/position)))))
               
(defn follow-rotation [^GameObject follower k]
  (if-let [^GameObject followed (state follower k)]
    (set! (.. follower transform rotation)
          (q* (.. followed transform rotation)
              (state follower :follow/rotation)))))

;follow+ shouldn't expect in its arguments a key for every hook
;but it is necessary since now the match is 1 hook to 1 key
;so the :pose (position|rotation) enum is not achieavable atm
(defn follow+ "er will follow ed, at a specific attr from enumap."
  [^GameObject er ^GameObject ed attr]
  (let [{:keys [position rotation scale]} enumap 
        k :follow ;@todo gen an unique key hashed from iid/hook
        enum (if (int? attr) attr (enumap attr))]
    (state+ er k ed)
    (state+ er :follow/position 
               (v3- (.. er transform position)
                    (.. ed transform position)))
    (state+ er :follow/rotation
               (q* (Quaternion/Inverse (.. ed transform rotation))
                   (.. er transform rotation)))
    (bit-match enum
      position (hook+ er :late-update k #'follow-position)
      rotation (hook+ er :late-update k #'follow-rotation))))
    
(defn follow- "er stops following ed, on specific attr."
  [^GameObject er ^GameObject ed attr]
  (state- er :follow)
  (hook- er :late-update :follow))

;if the camera is flowing around the player, it should also avoid
;trespassing obstacles otherwise it will block the line-of-sight
;for a dynamic camera there are usually 7 degrees of freedom
;vertical
;horizontal
;distance
;field-of-view
;pitch
;yaw
;(?)

;https://www.youtube.com/watch?v=C7307qRmlMI (50 Camera Mistakes)
; 4. Using a default camera distance likely to break LOS (10:48)
; 5. Allowing obstacles to break LOS from the side (11:41)
;    Detect obstacles with circular raycasts
;    Cinematography 30° rule (avoid jumpcut sensation)
; 8. Letting independent forces compete to push the camera (15:13)
;    Organize forces by degree-of-freedom at prioritizing by axis
; 9. Keeping narrow columns from breaking line-of-sight (16:31)
;    Tag obstacles that are allowed to break line-of-sight
;12. Swinging sideways when occluders come from behind (12:51)
;    Raycast behind the camera
;14. Using the same camera distance for all angles (20:36)
;    Use a spline to transition into a closeup
;    Worm's-eye leaves very little camera space
;    Bird's-eye reduces visibility, hiding horizon
;15. Using the same FOV for worm's eye and standard angles (21:56)
;16. Shifting pitch, distance, and FOV independently (22:40)
;    Distance and FOV should be derived from pitch, like gears
;
;17. Not cutting when the avatar passes through opaque areas (24:00)
;    i.e. a waterfall or some hidden passage
;21. Focusing only on the avatar (28:08)
;    Player need to see where they're going
;22. Relying on players to control the camera all the time (28:40)
;23. Leaving camera yaw alone while player is running (29:26)
;    Drift behind the avatar if they're running (unless walls)
;24. Making it hard to judge distances (30:28)
;25. Looking straight ahead as the avatar approaches a cliff (31:39)
;    Raycast looking for drops ahead the avatar
;26. Keeping the camera level when running on a slope (32:42)
;27. Misusing the "Rule of thirds" (37:26)
;    Framing things off-center can be pleasant (when still)
;    But don't pivot in-place, slide sideways to frame the avatar
;    Players use the center of the screen to aim!
;    Shadow of the Colossus is a exception (horse ride not= aim)
;28. Using the same logic for ground and air motion (35:28)
;    When jumping or flying look where to land
;    Modulate rule changes by the avatar's height above ground
;29. Relying entirely on procedural camera behaviours (36:34)
;    For special cases, us scripted "hints" to point yaw or pitch
;    Specially on small closed enviroments
;31. Rotating to look at nearby targets (39.17)
;    Move back or sideways to include targets in the view
;32. Translating to look at distant targets (40:22)
;    The only way to look at the sun is to rotate the camera
;33. Letting the avatar's own body occlude targets ahead (40:54)
;    A slight bird's eye view usually works fine
;34. Giving and taking control over the camera to the player (41:34)
;    Enable player to overwrite the hints whenever possible
;    Reduce the need for the player to control the camera
;35. Immediately applying a hint after the player control (42:12)
;    Let the camera rest for a moment or until the avatar moves
;37. Not providing a option to enable inverted controls (43:42) 
;39. Using linear sensitivity (45:37)
;    Analog sticks don't have much range
;    Use an "S" curve to allow players to make small adjustments     
;40. Letting the camera pivot drift too far (46:23)
;    Get smooth motions by letting the avatar drift slightly 
;    Limit how far the camera can drift
;41. Using a small field-of-view (47:15)
;    Beware simulation sickness (aka Portal 1 & 2 wtf)
;42. Rapidly shifting field-of-view (49:39)
;    Foward-backward motion = simulation sickness
;45. Translating or rotating up and down when jumping (52:02)
;    The camera can ignore temporary vertical motion
;    When moving from platforms the camera can lazily adapt
;46. Rapidly transitioning to a new camera position (53:16)
;    Cut if necessary
;47. Maintaning pitch speed until hitting the pitch limit
;    Stopping abruptly triggers sickness
;    Use an inverted "S" curve (or a low pass filter step response)
;END The devil is the details (Good luck!)

(do 1)