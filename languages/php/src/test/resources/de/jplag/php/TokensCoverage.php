<?php
namespace Demo\App;

use Demo\Support\Toolset;

#[Example]
class ExampleService extends BaseService implements ServiceContract {
    const VERSION = '1.0';

    public function run(array $items): void {
        global $globalCounter;
        static $invocations;

        include 'bootstrap.php';
        require_once 'helpers.php';

        $instance = new Worker();

        if ($instance->isReady()) {
            echo $instance->status();
        } elseif ($instance === null) {
            for ($i = 0; $i < 3; $i++) {
                continue;
            }
        } else {
            foreach ($items as $item) {
                while ($item->active()) {
                    do {
                        break;
                    } while ($item->shouldRetry());
                }
            }
        }

        switch ($instance->mode()) {
            case 'alpha':
                print 'A';
                break;
            default:
                throw new \RuntimeException();
        }

        try {
            return;
        } catch (\Throwable $exception) {
            throw $exception;
        } finally {
            $matching = match ($instance->mode()) {
                1 => 'one',
                default => 'other',
            };
        }
    }
}

interface ServiceContract {
    public function execute(): void;
}

trait SharedBehaviour {
    public function helper(): \Generator {
        yield from generator();
    }
}

enum Selection {
    case FIRST;
    case SECOND;
}

function generator(): iterable {
    yield 1;
}
?>
