(ns danuraidb.fellowshipcore
  (:require
    [reagent.core :as r]
    [danuraidb.fellowshipview :as view]))
    
(r/render [view/page] (.getElementById js/document "fellowship"))