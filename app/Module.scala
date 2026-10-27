import com.google.inject.AbstractModule
import services.TickerService

class Module  extends AbstractModule{

  override def configure(): Unit = {
    bind(classOf[TickerService]).asEagerSingleton()
  }

}
