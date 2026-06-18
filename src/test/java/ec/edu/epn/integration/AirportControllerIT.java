package ec.edu.epn.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.epn.dto.AirportRequest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc //Permite configurar de manera automática el MockMvc - para realizar pruebas de integración
@Transactional // <-- Esta anotación hace la magia de limpiar la BD después de cada test
public class AirportControllerIT {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldCreateAirport() throws Exception {
        // Arrange
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("UIO");
        airportRequest.setCity("Quito");
        airportRequest.setCountry("Ecuador");

        // Act - Asserts
        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isCreated()
            ).andExpect(jsonPath("$.name").value("Aeropuerto Mariscal Sucre")
            ).andExpect(jsonPath("$.code").value("UIO")
            ).andExpect(jsonPath("$.city").value("Quito")
            ).andExpect(jsonPath("$.country").value("Ecuador")
            ).andExpect(jsonPath("$.id").isNumber());
    }

     //es mejor usar thrwos para dejar a JUnit que maneje las excepciones y asi evitar usar try-catch aya que a veces puede ser mala practica
    @Test
    public void shouldDeleteAirport() throws Exception {
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Santiago de Chile");
        airportRequest.setCode("SCL");
        airportRequest.setCity("Santiago");
        airportRequest.setCountry("Chile");

        /* Se modifica para usar el método createAirport, que retorna el JSON de respuesta como String, para luego extraer el id del aeropuerto creado
        // String response = mockMvc.perform(post("/api/airports")
        //         .contentType(MediaType.APPLICATION_JSON)
        //         .content(objectMapper.writeValueAsString(airportRequest))
        //     ).andExpect(status().isCreated())
        //     .andReturn().getResponse().getContentAsString(); */
        String response = createAirport(airportRequest);

        Long id = objectMapper.readTree(response).get("id").asLong(); //readTree, permite deserializar el JSON y acceder a sus campos

        mockMvc.perform(delete("/api/airports/" + id))
            .andExpect(status().isNoContent());

        //una busqueda por id porque si se borro ya no se debe encontrar con ese id, debe salir 404(q es Not Found)
        mockMvc.perform(get("/api/airports/" + id))
            .andExpect(status().isNotFound());
    }


    @Test
    public void shouldUpdateAirport() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Santiago de Chile");
        airportRequest.setCode("SCL");
        airportRequest.setCity("Santiago");
        airportRequest.setCountry("Chile");

        String response = createAirport(airportRequest);
        Long id = objectMapper.readTree(response).get("id").asLong();

        //para hacer un cambio, para mandar el update con este cambio
        airportRequest.setName("Aeropuerto Santiago de Chile");

        mockMvc.perform(put("/api/airports/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isOk() //si se hace de la manera correcta se va a esperar un code 200 (OK)
            ).andExpect(jsonPath("$.name").value("Aeropuerto Santiago de Chile")); //se espera que el nombre del aeropuerto se haya actualizado correctamente, y se verifica con jsonPath que el campo name del JSON de respuesta tenga el valor actualizado
    }


    @Test
    public void shouldRejectDuplicateAirportCode() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("UIO");
        airportRequest.setCity("Quito");
        airportRequest.setCountry("Ecuador");

        // Act - Asserts
        createAirport(airportRequest);

        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isBadRequest());
    }


    @Test
    public void shouldFindAllAirports() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("UIO");
        airportRequest.setCity("Quito");
        airportRequest.setCountry("Ecuador");

        String response = createAirport(airportRequest);

        AirportRequest airportRequest2 = new AirportRequest();
        airportRequest2.setName("Santiago de Chile");
        airportRequest2.setCode("SCL");
        airportRequest2.setCity("Santiago");
        airportRequest2.setCountry("Chile");

        String response2 = createAirport(airportRequest2);

        Long id1 = objectMapper.readTree(response).get("id").asLong();
        Long id2 = objectMapper.readTree(response2).get("id").asLong();

        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id1)) // Primer elemento de la lista
                .andExpect(jsonPath("$[1].id").value(id2)); // Segundo elemento de la lista
    }


    @Test
    public void shouldFindAirportById() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("UIO");
        airportRequest.setCity("Quito");
        airportRequest.setCountry("Ecuador");

        String response = createAirport(airportRequest);

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/airports/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isOk()
            ).andExpect(jsonPath("$.id").value(id));
    }


    @Test
    public void shouldFindAirportByCode() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("UIO");
        airportRequest.setCity("Quito");
        airportRequest.setCountry("Ecuador");

        String response = createAirport(airportRequest);

        String code = objectMapper.readTree(response).get("code").asText();

        mockMvc.perform(get("/api/airports/code/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isOk()
            ).andExpect(jsonPath("$.code").value(code));
    }


    @Test
    public void shouldReturn404WhenAirportNotFound() throws Exception{
        mockMvc.perform(get("/api/airports/code/" + "GYE")
            ).andExpect(status().isNotFound());
    }


    @Test
    public void shouldRejectInvalidAirportRequest() throws Exception{
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCode("");
        
        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isBadRequest());
    }

    //Puede ser que se necesite el id, puede que se necesite otros valores
    //Con String se asegura que se retorna el json de respuesta
    private String createAirport(AirportRequest airportRequest) throws Exception{
        return mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest))
            ).andExpect(status().isCreated())
             .andReturn().getResponse().getContentAsString();
    }
}
