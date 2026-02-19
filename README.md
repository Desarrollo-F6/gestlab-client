# gestlab-client
Software que se ejecutara en cada equipo de los laboratorios de computacion


# EJECUTABLE

Para obtener el .jar y ejecutarlo en alguna pc, se debe:

1.- ir a Project Structure
2.- ir a Artifacts
3.- darle a el signo +, luego a JAR y luego en "From modules wih dependencies..."
4.- en Main Class seleccionar la clase principal (Al haber una sola se selecciona com.f6.Main), luego presionar "OK"
5.- ir a la pestaña de Gradle, task, shadow, ejecutar shadowajr, eso generara un jar que incluya todas las dependencias
6.- ir a la ubicacion del JAR (al ser un proyecto en Java Gradle, se debera de dirigirse a la ubicacion: build/libs), es el que al final tiene -all
7.- LISTO el ejecutable es Gestlab-Client.jar
