(ns calculadorapi.conexoes
    (:require [clj-http.client :as client]
              [cheshire.core :as json]))

(def BURNED_API_URL "https://api.api-ninjas.com/v1/caloriesburned?activity=%s")
(def BURNED_API_KEY "YIgc/1iXCMsI/SmYbZ0VQw==X3HElntYRgfcow86")

(def TRANSLATE_API_URL "https://google-api31.p.rapidapi.com/gtranslate")
(def TRANSLATE_API_KEY "4af24503a6mshadf3b1d33832a9cp1118fdjsnfd8ac5bc0d1d")
(def TRANSLATE_API_HOST "google-api31.p.rapidapi.com")

(defn pegar-gasto-calorias [atividade]
    (let [url (format BURNED_API_URL atividade)
          body (client/get url {:headers {:X-Api-Key BURNED_API_KEY}})]
        (if (= 200 (:status body))
            (json/parse-string (:body body) true)
            {:error "Erro ao acessar a API de queima de calorias"})      
    )
)

(defn pegar-ganho-calorias [atividade]
  
)

(defn traduzir [texto origem destino]
  (let [resposta (client/post TRANSLATE_API_URL
                              {:headers {"x-rapidapi-key" TRANSLATE_API_KEY
                                         "x-rapidapi-host" TRANSLATE_API_HOST}
                               :content-type :json
                               :form-params {:text texto
                                             :to destino
                                             :from_lang origem}})]
    (if (= 200 (:status resposta))
      (json/parse-string (:body resposta) true)
      {:error "Erro ao acessar a API de tradução"})))