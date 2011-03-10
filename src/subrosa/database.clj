(ns subrosa.database
  (:refer-clojure :exclude [get])
  (:import [java.util UUID]))

(defonce db (ref {}))

(defn ensure-id [m]
  (if (:id m)
    m
    (assoc m :id (str (UUID/randomUUID)))))

(defn add-index* [db table field-or-fields]
  (update-in db [table :indices] (comp set conj) field-or-fields))

(defn add-index [table field-or-fields]
  (dosync
   (alter db add-index* table field-or-fields)))

(defn get-indices* [db table]
  (conj (get-in db [table :indices]) :id))

(defn get-indices [table]
  (dosync
   (get-indices* @db table)))

(defn get*
  ([db table]
     (vals (get-in db [table :data :id])))
  ([db table column value]
     (get-in db [table :data column value])))

(defn get
  ([table]
     (get* @db table))
  ([table column value]
     (get* @db table column value)))

(defn delete* [db table m]
  (update-in db [table] assoc
             :data (reduce (fn [a [k v]]
                             (update-in a [k] dissoc v))
                           (get-in db [table :data])
                           (select-keys m (get-indices* db table)))))

(defn delete [table id]
  (dosync
   (let [m (get table :id id)]
     (alter db delete* table m))))

(defn put* [db table m]
  (update-in db [table] assoc
             :data (reduce (fn [a [k v]]
                             (update-in a [k] assoc v m))
                           (get-in db [table :data])
                           (select-keys m (get-indices* db table)))))

(defn put [table m]
  (dosync
   (let [m (ensure-id m)]
     (alter db (fn [db]
                 (-> db
                     (delete* table m)
                     (put* table m)))))))
